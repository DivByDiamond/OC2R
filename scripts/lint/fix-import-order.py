#!/usr/bin/env python3
"""Sort the import blocks of the shared tree so every multiversion variant passes
Checkstyle's `CustomImportOrder` (STATIC###THIRD_PARTY_PACKAGE, alphabetical).

Why this is not a plain "sort the imports" job
----------------------------------------------
The shared tree is the VCS state of Minecraft 1.21.1; the 26.x nodes lint the
Stonecutter-resolved trees. A conditional block (`//? if >=26.1 { ... *///?}`)
carries a different set of imports per version, and the Stonecutter replacements
rename some of them (`ResourceLocation` -> `Identifier`, `GuiGraphics` ->
`GuiGraphicsExtractor`). An import therefore has one name per variant, and the
order has to be correct for *all* variants at once. When the imports of one
conditional block need different positions in different versions, the block is
split into several blocks with the same condition.

The comparator follows Checkstyle's `CustomImportOrderCheck.compareImports`:
split on ".", compare the segments pairwise (case sensitive), shorter path
first when all common segments are equal. It lives in `import_order.py`, which
the other scripts of this directory share, so that the tool that reports and
the tool that fixes agree on what "sorted" means.

Files whose import region contains an `if/else` branch pair are skipped: their
branches are complete alternatives of each other and must stay adjacent.

Usage:
    fix-import-order.py --root . [--variant 26.1.2=DIR --variant 26.2=DIR]
                         [--apply] [FILE ...]
"""

from __future__ import annotations

import argparse
import functools
import pathlib
import re
import sys
from dataclasses import dataclass, field

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from import_order import compare_imports  # noqa: E402

BLOCK_OPEN_RE = re.compile(r"^\s*//\?\s*(if|else)\b(.*?)\{\s*$")
BLOCK_ELSE_RE = re.compile(r"^\s*(?:\*/)?//\?\}\s*else\s*\{\s*$")
BLOCK_CLOSE_RE = re.compile(r"^\s*(?:\*/)?//\?\}?\s*$")
IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([\w.]+(?:\.\*)?)\s*;\s*$")
COMMENTED_IMPORT_RE = re.compile(r"^\s*/\*\s*import\s+(static\s+)?([\w.]+(?:\.\*)?)\s*;\s*$")

BASE_VERSION = "1.21.1"


# --------------------------------------------------------------------------- #
# version conditions
# --------------------------------------------------------------------------- #


def parse_version(text: str) -> tuple[int, ...]:
    parts: list[int] = []
    for chunk in text.split("."):
        digits = "".join(c for c in chunk if c.isdigit())
        parts.append(int(digits) if digits else 0)
    return tuple(parts)


def version_at_least(version: str, threshold: str) -> bool:
    left = parse_version(version)
    right = parse_version(threshold)
    length = max(len(left), len(right))
    left += (0,) * (length - len(left))
    right += (0,) * (length - len(right))
    return left >= right


def compare_versions(left: str, right: str) -> int:
    left_parts = parse_version(left)
    right_parts = parse_version(right)
    length = max(len(left_parts), len(right_parts))
    left_parts += (0,) * (length - len(left_parts))
    right_parts += (0,) * (length - len(right_parts))
    return (left_parts > right_parts) - (left_parts < right_parts)


def evaluate_condition(condition: str, version: str) -> bool:
    """Evaluate a Stonecutter condition such as `>=26.1 && <26.2`."""
    text = condition.strip()
    if text.startswith("!"):
        return not evaluate_condition(text[1:], version)
    for part in text.split("&&"):
        part = part.strip()
        if part.startswith(">="):
            if compare_versions(version, part[2:].strip()) < 0:
                return False
        elif part.startswith("<="):
            if compare_versions(version, part[2:].strip()) > 0:
                return False
        elif part.startswith(">"):
            if compare_versions(version, part[1:].strip()) <= 0:
                return False
        elif part.startswith("<"):
            if compare_versions(version, part[1:].strip()) >= 0:
                return False
        else:
            raise ValueError(f"unsupported condition: {condition!r}")
    return True


# --------------------------------------------------------------------------- #
# checkstyle comparison
# --------------------------------------------------------------------------- #


def cmp_key(names: dict[int, dict[str, str]], variant: str):
    def compare(left: int, right: int) -> int:
        return compare_imports(names[left][variant], names[right][variant])

    return functools.cmp_to_key(compare)


# --------------------------------------------------------------------------- #
# shared tree parsing
# --------------------------------------------------------------------------- #


@dataclass
class Import:
    name: str
    is_static: bool
    condition: str | None
    negated: bool
    line: int
    text: str


@dataclass
class Region:
    start: int
    end: int
    imports: list[Import] = field(default_factory=list)
    has_else: bool = False


def parse_region(text: str) -> Region | None:
    """Parse the import region of a shared-tree file.

    Returns None when the file has no imports, raises ValueError for regions the
    tool does not handle (an `if/else` pair or foreign lines inside the region).
    """
    lines = text.splitlines()
    positions = [
        number
        for number, line in enumerate(lines)
        if IMPORT_RE.match(line) or COMMENTED_IMPORT_RE.match(line)
    ]
    if not positions:
        return None

    region = Region(start=min(positions), end=max(positions))
    # The region must start at the enclosing conditional block header, otherwise
    # the imports of that block would look unconditional.
    while region.start > 0 and (
        BLOCK_OPEN_RE.match(lines[region.start - 1]) or BLOCK_ELSE_RE.match(lines[region.start - 1])
    ):
        region.start -= 1
    stack: list[tuple[str | None, bool]] = []
    index = region.start
    while index <= region.end:
        line = lines[index]
        stripped = line.strip()
        open_match = BLOCK_OPEN_RE.match(line)
        if open_match:
            stack.append((open_match.group(2).strip(), False))
            index += 1
            continue
        if BLOCK_ELSE_RE.match(line):
            region.has_else = True
            stack.append((None, True))
            index += 1
            continue
        if BLOCK_CLOSE_RE.match(line):
            if stack:
                stack.pop()
            index += 1
            continue
        if stripped and not (
            IMPORT_RE.match(line)
            or COMMENTED_IMPORT_RE.match(line)
            or stripped.startswith("//?")
            or stripped.startswith("*///?")
            or stripped.startswith("/*")
        ):
            raise ValueError(f"foreign line {index + 1}: {stripped!r}")

        condition: str | None = None
        negated = False
        if stack:
            condition, negated = stack[-1]
            if condition is None:
                enclosing = next((c for c, n in reversed(stack) if c is not None), None)
                condition = enclosing
                negated = True

        plain = IMPORT_RE.match(line)
        commented = COMMENTED_IMPORT_RE.match(line)
        if plain or commented:
            match = plain or commented
            region.imports.append(
                Import(
                    name=match.group(2),
                    is_static=bool(match.group(1)),
                    condition=condition,
                    negated=negated,
                    line=index,
                    text=line.strip(),
                )
            )
        index += 1

    # A conditional block may still be open at the end of the region; the
    # closing marker belongs to the region, otherwise the rewritten block would
    # leave the original closer behind.
    while stack:
        if region.end + 1 >= len(lines):
            raise ValueError("unterminated conditional block")
        following = lines[region.end + 1]
        if BLOCK_ELSE_RE.match(following) or BLOCK_CLOSE_RE.match(following):
            region.end += 1
            if BLOCK_ELSE_RE.match(following):
                stack.append((None, True))
            else:
                stack.pop()
            continue
        raise ValueError(f"foreign line {region.end + 2}: {following.strip()!r}")
    return region


def import_active(item: Import, version: str) -> bool:
    if item.condition is None:
        return True
    holds = evaluate_condition(item.condition, version)
    return not holds if item.negated else holds


def commented_in_base(item: Import, base: str) -> bool:
    """True when the import is inside a `/* ... *///?` block in the shared tree."""
    if item.condition is None:
        return False
    holds = evaluate_condition(item.condition, base)
    if item.negated:
        holds = not holds
    return not holds


# --------------------------------------------------------------------------- #
# ordering
# --------------------------------------------------------------------------- #


def required_order(
    imports: list[Import], names: dict[int, dict[str, str]], variant: str
) -> list[int]:
    """The import indices Checkstyle expects to see in this variant."""
    visible = [i for i, item in enumerate(imports) if import_active(item, variant)]
    statics = [i for i in visible if imports[i].is_static]
    regular = [i for i in visible if not imports[i].is_static]
    key = cmp_key(names, variant)
    return sorted(statics, key=key) + sorted(regular, key=key)


def topological_order(nodes: list[int], edges: dict[int, set[int]]) -> list[int]:
    indegree = {node: 0 for node in nodes}
    for targets in edges.values():
        for target in targets:
            indegree[target] += 1
    ready = sorted(node for node in nodes if indegree[node] == 0)
    order: list[int] = []
    while ready:
        node = ready.pop(0)
        order.append(node)
        for target in sorted(edges.get(node, ())):
            indegree[target] -= 1
            if indegree[target] == 0:
                ready.append(target)
                ready.sort()
    if len(order) != len(nodes):
        cyclic = sorted(set(nodes) - set(order))
        raise ValueError(f"cyclic import order constraints: {cyclic}")
    return order


def plan_order(
    imports: list[Import], names: dict[int, dict[str, str]], variants: list[str]
) -> list[int]:
    """One order that satisfies every variant."""
    nodes = list(range(len(imports)))
    edges: dict[int, set[int]] = {node: set() for node in nodes}
    for variant in variants:
        ordered = required_order(imports, names, variant)
        for before, after in zip(ordered, ordered[1:]):
            edges[before].add(after)
    return topological_order(nodes, edges)


def order_is_valid(
    imports: list[Import], names: dict[int, dict[str, str]], variants: list[str]
) -> bool:
    """True when the current order already satisfies every variant."""
    for variant in variants:
        visible = [i for i, item in enumerate(imports) if import_active(item, variant)]
        expected = required_order(imports, names, variant)
        if visible != expected:
            return False
    return True


# --------------------------------------------------------------------------- #
# emitting
# --------------------------------------------------------------------------- #


def render(imports: list[Import], order: list[int], base: str) -> list[str]:
    """Render the new import region.

    Consecutive imports that share a condition are merged into one conditional
    block; a block whose imports need different positions is therefore split.
    The static imports form their own group and come first, separated by one
    empty line, as Checkstyle's `CustomImportOrder` requires.
    """
    statics = [i for i in order if imports[i].is_static]
    regular = [i for i in order if not imports[i].is_static]
    out: list[str] = []
    if statics:
        out.extend(render_group(imports, statics, base))
        out.append("")
    out.extend(render_group(imports, regular, base))
    return out


def render_group(imports: list[Import], order: list[int], base: str) -> list[str]:
    out: list[str] = []
    index = 0
    while index < len(order):
        item = imports[order[index]]
        if item.condition is None:
            out.append(import_line(item))
            index += 1
            continue
        run = [item]
        index += 1
        while index < len(order):
            candidate = imports[order[index]]
            if candidate.condition != item.condition or candidate.negated != item.negated:
                break
            run.append(candidate)
            index += 1
        out.extend(render_block(run, base))
    return out


def import_line(item: Import) -> str:
    """The import statement of an entry, keeping the static modifier."""
    return ("import static " if item.is_static else "import ") + item.name + ";"


def render_block(run: list[Import], base: str) -> list[str]:
    condition = run[0].condition
    negated = run[0].negated
    holds_in_base = evaluate_condition(condition, base)
    if negated:
        holds_in_base = not holds_in_base
    lines = [f"//? if {condition} {{"]
    if holds_in_base:
        lines.extend(import_line(item) for item in run)
        lines.append("//?}")
    else:
        # A conditional block is commented out as a whole: the `/*` opens the
        # comment on the first line and `*///?}` closes it on the last.
        for position, item in enumerate(run):
            prefix = "/*" if position == 0 else ""
            lines.append(f"{prefix}{import_line(item)}")
        lines.append("*///?}")
    return lines


# --------------------------------------------------------------------------- #
# variant names
# --------------------------------------------------------------------------- #


def visible_imports(path: pathlib.Path) -> list[str]:
    """Import names of a resolved variant file, ignoring commented-out ones."""
    out: list[str] = []
    in_comment = False
    for line in path.read_text(encoding="utf-8").splitlines():
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
            out.append(match.group(2))
    return out


def resolve_names(
    imports: list[Import],
    base: str,
    variants: list[tuple[str, pathlib.Path]],
) -> dict[int, dict[str, str]] | None:
    """Map every import to its resolved name per variant."""
    names: dict[int, dict[str, str]] = {}
    base_visible = [i for i, item in enumerate(imports) if import_active(item, base)]
    for index, item in zip(base_visible, (item.name for item in imports if import_active(item, base))):
        names.setdefault(index, {})[base] = item
    for variant, directory in variants:
        resolved = visible_imports(directory)
        active = [i for i, item in enumerate(imports) if import_active(item, variant)]
        if len(active) != len(resolved):
            return None
        for index, name in zip(active, resolved):
            names.setdefault(index, {})[variant] = name
    return names


# --------------------------------------------------------------------------- #
# main
# --------------------------------------------------------------------------- #


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
    parser.add_argument("--base", default=BASE_VERSION)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("files", nargs="*")
    args = parser.parse_args()

    root = pathlib.Path(args.root).resolve()
    variants = []
    for spec in args.variant:
        name, _, directory = spec.partition("=")
        variants.append((name, (root / directory).resolve()))

    shared_root = root / "src/main/java"
    if args.files:
        files = [pathlib.Path(f).resolve() for f in args.files]
    else:
        files = sorted(shared_root.rglob("*.java"))

    # The build excludes these packages from the lint source sets.
    excluded = ("/jcodec/", "/gametest/")

    changed = 0
    skipped: list[tuple[str, str]] = []
    for shared in files:
        rel_path = str(shared)
        if any(part in rel_path for part in excluded):
            continue
        text = shared.read_text(encoding="utf-8")
        try:
            region = parse_region(text)
        except ValueError as error:
            skipped.append((str(shared.relative_to(root)), str(error)))
            continue
        if region is None:
            continue
        if region.has_else:
            skipped.append((str(shared.relative_to(root)), "if/else branch pair"))
            continue

        rel = shared.relative_to(shared_root)
        variant_paths = [
            (name, directory / rel) for name, directory in variants if (directory / rel).exists()
        ]
        names = resolve_names(region.imports, args.base, variant_paths)
        if names is None:
            skipped.append((str(shared.relative_to(root)), "cannot align variant imports"))
            continue

        all_variants = [args.base] + [name for name, _ in variant_paths]
        if order_is_valid(region.imports, names, all_variants):
            continue
        try:
            order = plan_order(region.imports, names, all_variants)
        except ValueError as error:
            skipped.append((str(shared.relative_to(root)), str(error)))
            continue

        if order == list(range(len(region.imports))):
            continue

        lines = text.splitlines()
        block = render(region.imports, order, args.base)
        new_lines = lines[: region.start] + block + lines[region.end + 1 :]
        new_text = "\n".join(new_lines) + ("\n" if text.endswith("\n") else "")
        if new_text == text:
            continue
        changed += 1
        print(f"fix {shared.relative_to(root)}")
        if args.apply:
            shared.write_text(new_text, encoding="utf-8")

    print(f"\nchanged: {changed}, skipped: {len(skipped)}")
    for name, reason in skipped:
        print(f"  skip {name}: {reason}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
