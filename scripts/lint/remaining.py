#!/usr/bin/env python3
"""List the files that still violate the lint rules, per variant.

Usage: remaining.py --report build/cs-scan/neo2612.xml --root .
"""

from __future__ import annotations

import argparse
import pathlib
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", action="append", required=True)
    parser.add_argument("--root", default=".")
    args = parser.parse_args()

    root = pathlib.Path(args.root).resolve()
    for report in args.report:
        tree = ET.parse(report).getroot()
        print(f"== {report}")
        total = 0
        for entry in tree.findall(".//file"):
            errors = entry.findall(".//error")
            if not errors:
                continue
            total += len(errors)
            path = pathlib.Path(entry.get("name"))
            try:
                rel = path.relative_to(root)
            except ValueError:
                rel = path
            kinds = sorted({e.get("source", "").split("/")[-1].replace("Check", "") for e in errors})
            print(f"   {len(errors):3d}  {rel}  [{', '.join(kinds)}]")
        print(f"   total errors: {total}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
