#!/usr/bin/env python3
"""Analyse the import blocks of the shared tree against the per-version variants.

The NeoForge 1.21.1 node lints the shared tree as-is; the 26.x nodes lint the
Stonecutter-resolved trees. Both must satisfy Checkstyle's
`CustomImportOrder` (STATIC###THIRD_PARTY_PACKAGE, alphabetical inside a group),
so a fix in the shared tree has to be correct for every variant at once.

This tool prints, per file, the import order each variant sees and what it would
have to be, so the reordering can be derived instead of guessed.

The expected order is the one Checkstyle computes: the static group first, then
the regular group, each sorted with the segment comparator of
`import_order.compare_imports` (not with `sorted()`). Imports inside a
`/* ... */` block are ignored: in the shared tree those are the branches of a
Stonecutter conditional block that the linted version does not use.

Usage: analyse-imports.py [--root DIR] [--variant NAME=DIR ...] [FILE ...]
"""

from __future__ import annotations

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from import_order import sorted_imports, visible_imports  # noqa: E402

STATIC_GROUP = 0
REGULAR_GROUP = 1


def parse_imports(path: pathlib.Path) -> list[tuple[int, str, bool]]:
    """Return (line number, import path, is_static) for the visible imports."""
    return visible_imports(path.read_text(encoding="utf-8").splitlines())


def variant_imports(path: pathlib.Path) -> list[tuple[int, str, bool]]:
    """Same as parse_imports, for a resolved variant tree."""
    return parse_imports(path)


def group_of(is_static: bool) -> int:
    return STATIC_GROUP if is_static else REGULAR_GROUP


def sorted_targets(imports: list[tuple[int, str, bool]]) -> list[str]:
    """Checkstyle's expectation: static group first, then alphabetical by segment."""
    statics = sorted_imports([p for _, p, s in imports if s])
    regular = sorted_imports([p for _, p, s in imports if not s])
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

    # The build excludes these packages from the lint source sets
    # (`exclude("**/jcodec/**", "**/gametest/**")` in build.gradle.kts), so the tool
    # must not report them either.
    excluded = ("/jcodec/", "/gametest/")

    broken = 0
    for shared in shared_files:
        rel = shared.relative_to(root)
        if any(part in "/" + rel.as_posix() for part in excluded):
            continue
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
