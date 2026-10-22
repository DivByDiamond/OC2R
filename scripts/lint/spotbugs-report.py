#!/usr/bin/env python3
"""Report the SpotBugs findings that are not covered by the checked-in baseline.

The baseline (`config/spotbugs/baseline.xml`) lists the findings the project
accepts for now; a new finding that is not in it fails `spotbugsMain`
(`ignoreFailures` is false). This tool does **not** write to the baseline: the
project's convention (see commit f5a4a17b) is to fix a finding or to suppress
it at the finding site with a justification, not to add it to the baseline.

The tool prints:

* the findings of the given reports that the baseline does not cover -- the
  actionable list, with type, class, method, field and line;
* duplicate `instanceHash` values inside the baseline: SpotBugs matches a
  finding against the baseline by hash, so two findings with the same hash are
  covered by a single baseline entry, and a *new* occurrence of an already
  baselined pattern in the same class/method/field is accepted silently;
* baseline entries that no longer occur in the given reports (stale entries).

Usage: spotbugs-report.py <report.xml> [<report.xml> ...]
"""

from __future__ import annotations

import collections
import pathlib
import sys
import xml.etree.ElementTree as ET

BASELINE = pathlib.Path("config/spotbugs/baseline.xml")


def describe(bug: ET.Element) -> str:
    cls = bug.find("Class")
    method = bug.find("Method")
    field = bug.find("Field")
    source = bug.find("SourceLine")
    return (
        f"{bug.get('type', '?'):<42} "
        f"{(cls.get('classname') if cls is not None else '?'):<48} "
        f"{(method.get('name') if method is not None else '-'):<24} "
        f"{(field.get('name') if field is not None else '-'):<20} "
        f":{source.get('start') if source is not None else '-'}"
    )


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2

    baseline_root = ET.parse(BASELINE).getroot()
    baseline_bugs = baseline_root.findall("BugInstance")
    baseline_hashes = collections.Counter(bug.get("instanceHash") for bug in baseline_bugs)

    findings: list[ET.Element] = []
    for report in sys.argv[1:]:
        path = pathlib.Path(report)
        if not path.exists():
            print(f"skip (missing): {report}")
            continue
        findings.extend(ET.parse(path).getroot().findall("BugInstance"))

    uncovered = [bug for bug in findings if bug.get("instanceHash") not in baseline_hashes]
    print(f"findings in the reports: {len(findings)}")
    print(f"not covered by the baseline: {len(uncovered)}")
    for bug in uncovered:
        print(f"  {describe(bug)}")

    duplicates = {hash_: count for hash_, count in baseline_hashes.items() if count > 1}
    print(f"\nduplicate instanceHash in the baseline: {len(duplicates)}")
    for hash_, count in duplicates.items():
        entries = [bug for bug in baseline_bugs if bug.get("instanceHash") == hash_]
        print(f"  {count}x {hash_}")
        for bug in entries:
            print(f"      {describe(bug)}")

    seen = {bug.get("instanceHash") for bug in findings}
    stale = [bug for bug in baseline_bugs if bug.get("instanceHash") not in seen]
    print(f"\nbaseline entries not seen in the given reports: {len(stale)}")
    for bug in stale[:20]:
        print(f"  {describe(bug)}")
    if len(stale) > 20:
        print(f"  ... and {len(stale) - 20} more")

    return 1 if uncovered else 0


if __name__ == "__main__":
    sys.exit(main())
