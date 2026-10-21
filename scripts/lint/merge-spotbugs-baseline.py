#!/usr/bin/env python3
"""Merge the current SpotBugs findings into the checked-in baseline.

The baseline lists the findings the project accepts for now; new code that is
not in it fails `spotbugsMain` (ignoreFailures is false). This script adds the
findings of the given reports to `config/spotbugs/baseline.xml` and keeps the
formatting of the existing entries untouched, so the diff stays readable.

Usage: merge-spotbugs-baseline.py <report.xml> [<report.xml> ...]
"""

from __future__ import annotations

import pathlib
import re
import sys
import xml.etree.ElementTree as ET

BASELINE = pathlib.Path("config/spotbugs/baseline.xml")
INSTANCE_RE = re.compile(r"<BugInstance\b.*?</BugInstance>", re.DOTALL)


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2

    text = BASELINE.read_text(encoding="utf-8")
    existing = {bug.get("instanceHash") for bug in ET.fromstring(text).findall("BugInstance")}

    additions: list[str] = []
    added = 0
    for report in sys.argv[1:]:
        path = pathlib.Path(report)
        if not path.exists():
            print(f"skip (missing): {report}")
            continue
        report_text = path.read_text(encoding="utf-8")
        for match in INSTANCE_RE.finditer(report_text):
            block = match.group(0)
            key = ET.fromstring(block).get("instanceHash")
            if key in existing:
                continue
            existing.add(key)
            additions.append(block)
            added += 1

    if not additions:
        print("baseline: nothing to add")
        return 0

    marker = "</BugCollection>"
    index = text.rindex(marker)
    insertion = "\n".join(additions) + "\n"
    BASELINE.write_text(text[:index] + insertion + text[index:], encoding="utf-8")

    total = len(ET.fromstring(BASELINE.read_text(encoding="utf-8")).findall("BugInstance"))
    print(f"baseline: +{added} findings, {total} entries total")
    return 0


if __name__ == "__main__":
    sys.exit(main())
