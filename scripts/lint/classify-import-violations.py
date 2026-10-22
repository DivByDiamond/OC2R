#!/usr/bin/env python3
"""Classify the remaining import-order violations per variant.

For every file that Checkstyle flags, report which imports live inside a
Stonecutter conditional block (they may need a different position per version)
and which are plain (sortable in place).
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([\w.]+)\s*;\s*$")
COMMENTED_IMPORT_RE = re.compile(r"^\s*/\*\s*import\s+(static\s+)?([\w.]+)\s*;\s*$")
BLOCK_OPEN_RE = re.compile(r"^\s*//\?\s*(if|else)\b(.*?)\{\s*$")


def visible_imports(path: pathlib.Path) -> list[tuple[int, str, bool]]:
    out: list[tuple[int, str, bool]] = []
    in_comment = False
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        stripped = line.strip()
        if in_comment:
            if "*/" in stripped:
                in_comment = False
            continue
        if stripped.startswith("/*"):
            if "*/" not in stripped:
                in_comment = True
            continue
        match = IMPORT_RE.match(line)
        if match:
            out.append((number, match.group(2), bool(match.group(1))))
    return out


def conditional_imports(path: pathlib.Path) -> set[str]:
    """Names of imports that sit inside a `//?` conditional block."""
    names: set[str] = set()
    depth = 0
    for line in path.read_text(encoding="utf-8").splitlines():
        if BLOCK_OPEN_RE.match(line):
            depth += 1
            continue
        if depth and re.match(r"^\s*(\*///\?\}?)\s*$", line.strip()):
            depth -= 1
            continue
        if depth:
            match = IMPORT_RE.match(line) or COMMENTED_IMPORT_RE.match(line)
            if match:
                names.add(match.group(2))
    return names


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument("--report", required=True, help="checkstyle xml report")
    parser.add_argument("--variant", required=True, help="resolved sources of the variant")
    args = parser.parse_args()

    root = pathlib.Path(args.root).resolve()
    report = ET.parse(args.report).getroot()
    variant_root = root / args.variant

    conditional_files = 0
    plain_files = 0
    for entry in report.findall(".//file"):
        errors = entry.findall(".//error")
        if not errors:
            continue
        path = pathlib.Path(entry.get("name"))
        if not path.exists():
            continue
        shared = root / "src/main/java" / path.relative_to(variant_root)
        conditional = conditional_imports(shared)
        names = [name for _, name, _ in visible_imports(path)]
        touched = [
            name
            for error in errors
            for name in (
                re.findall(r"'([^']+)'", error.get("message", ""))[:1]
            )
        ]
        hits = [name for name in touched if name in conditional]
        print(f"{shared.relative_to(root)}")
        print(f"   errors={len(errors)} conditional_hits={len(hits)}/{len(touched)}")
        for name in touched[:6]:
            mark = "cond" if name in conditional else "PLAIN"
            print(f"     [{mark}] {name}")
        if hits:
            conditional_files += 1
        else:
            plain_files += 1
    print(f"\nfiles with only plain-import issues: {plain_files}")
    print(f"files touching conditional imports: {conditional_files}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
