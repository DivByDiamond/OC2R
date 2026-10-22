#!/usr/bin/env python3
"""Shared helpers of the import-order tooling.

The comparator is Checkstyle's `CustomImportOrderCheck.compareImports`: split
the path on ".", compare the segments pairwise (case sensitive), shorter path
first when all common segments are equal. It is **not** a plain string
comparison, so `sorted()` produces a different (wrong) order:

    net.minecraft.client.renderer.MultiBufferSource sorts **before**
    net.minecraft.client.renderer.item.ItemStackRenderState, because the
    fourth segment is compared case sensitively ("M" < "i").

The scripts of this directory share this module so that the tool that reports
and the tool that fixes agree on what "sorted" means.
"""

from __future__ import annotations

import functools
import re

IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([\w.]+(?:\.\*)?)\s*;\s*$")
COMMENTED_IMPORT_RE = re.compile(r"^\s*/\*\s*import\s+(static\s+)?([\w.]+(?:\.\*)?)\s*;\s*$")


def compare_imports(first: str, second: str) -> int:
    """Checkstyle's CustomImportOrderCheck.compareImports."""
    first_tokens = first.split(".")
    second_tokens = second.split(".")
    result = 0
    for left, right in zip(first_tokens, second_tokens):
        result = (left > right) - (left < right)
        if result != 0:
            break
    if result == 0:
        result = (len(first_tokens) > len(second_tokens)) - (len(first_tokens) < len(second_tokens))
    return result


def import_key():
    """A sort key that orders import paths the way Checkstyle does."""
    return functools.cmp_to_key(compare_imports)


def sorted_imports(names: list[str]) -> list[str]:
    """The order Checkstyle expects inside one import group."""
    return sorted(names, key=import_key())


def visible_imports(lines: list[str]) -> list[tuple[int, str, bool]]:
    """(line number, import path, is_static) of the imports that are not commented out.

    A `/* ... */` block hides the imports inside it. In the shared tree those are
    the branches of a Stonecutter conditional block that does not hold for the
    version the file is linted for, so they must not be counted.
    """
    out: list[tuple[int, str, bool]] = []
    in_comment = False
    for number, line in enumerate(lines, start=1):
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
