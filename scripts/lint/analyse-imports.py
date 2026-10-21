#!/usr/bin/env python3
"""Analyse the import blocks of the shared tree against the per-version variants.

The NeoForge 1.21.1 node lints the shared tree as-is; the 26.x nodes lint the
Stonecutter-resolved trees. Both must satisfy Checkstyle's
`CustomImportOrder` (STATIC###THIRD_PARTY_PACKAGE, alphabetical inside a group),
so a fix in the shared tree has to be correct for every variant at once.

This tool prints, per file, the import order each variant sees and what it would
have to be, so the reordering can be derived instead of guessed.

Usage: analyse-imports.py [--root DIR] [--variant NAME=DIR ...] [FILE ...]
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys

IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([\w.]+)\s*;")
COMMENTED_IMPORT_RE = re.compile(r"^\s*/\*\s*import\s+(static\s+)?([\w.]+)\s*;")

STATIC_GROUP = 0
REGULAR_GROUP = 1


def parse_imports(path: pathlib.Path) -> list[tuple[int, str, bool]]:
    """Return (line number, import path, is_static) for the visible imports."""
    out: list[tuple[int, str, bool]] = []
    text = path.read_text(encoding="utf-8")
    for number, line in enumerate(text.splitlines(), start=1):
        match = IMPORT_RE.match(line)
        if match:
            out.append((number, match.group(2), bool(match.group(1))))
    return out


def variant_imports(path: pathlib.Path) -> list[tuple[int, str, bool]]:
    """Same as parse_imports, but ignores `/*`-prefixed (inactive) imports."""
    out: list[tuple[int, str, bool]] = []
    text = path.read_text(encoding="utf-8")
    for number, line in enumerate(text.splitlines(), start=1):
        if COMMENTED_IMPORT_RE.match(line):
            continue
        match = IMPORT_RE.match(line)
        if match:
            out.append((number, match.group(2), bool(match.group(1))))
    return out


def group_of(is_static: bool) -> int:
    return STATIC_GROUP if is_static else REGULAR_GROUP


def sorted_targets(imports: list[tuple[int, str, bool]]) -> list[str]:
    """Checkstyle's expectation: static group first, then alphabetical."""
    statics = sorted(p for _, p, s in imports if s)
    regular = sorted(p for _, p, s in imports if not s)
    return statics + regular


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument(
        "--variant",
        action="append",
        default=[],
        metavar="NAME=DIR",
        help="variant name and directory holding its resolved sources",
    )
    parser.add_argument("files", nargs="*")
    args = parser.parse_args()

    root = pathlib.Path(args.root).resolve()
    variants = []
    for spec in args.variant:
        name, _, directory = spec.partition("=")
        variants.append((name, (root / directory).resolve()))

    shared_root = root / "src/main/java"
    if args.files:
        shared_files = [pathlib.Path(f).resolve() for f in args.files]
    else:
        shared_files = sorted(shared_root.rglob("*.java"))

    broken = 0
    for shared in shared_files:
        rel = shared.relative_to(root)
        per_variant = []
        for name, directory in variants:
            candidate = directory / rel.relative_to("src/main/java")
            if not candidate.exists():
                continue
            per_variant.append((name, candidate))

        views = [("1.21.1", shared, parse_imports(shared))]
        for name, candidate in per_variant:
            views.append((name, candidate, variant_imports(candidate)))

        for name, path, imports in views:
            target = sorted_targets(imports)
            current = [p for _, p, _ in imports]
            if current != target:
                broken += 1
                print(f"{rel} [{name}]")
                for want, got in zip(target, current):
                    mark = "  " if want == got else "!!"
                    print(f"   {mark} want={want:<70} got={got}")
                if len(target) != len(current):
                    print(f"   length differs: want {len(target)} got {len(current)}")
                print()
    print(f"files with a non-sorted import block: {broken}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
