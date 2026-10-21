#!/usr/bin/env python3
"""Resolve a shared-tree Java file for one Minecraft version.

This mirrors what Stonecutter does for the 26.x nodes, so a single file can be
checked without running Gradle:

* a `//? if COND {` branch keeps its content when COND holds for the target
  version and is commented out otherwise (`/*` on the first content line and
  `*///?}` as the closer);
* the `else` branch of a block is the negated condition;
* the Stonecutter replacements from stonecutter.gradle.kts are applied
  (`ResourceLocation` -> `Identifier` and `GuiGraphics` -> `GuiGraphicsExtractor`
  on 26.x, the reverse below 26.1).

Usage:
    variant-view.py --version 26.1.2 src/main/java/.../Foo.java
    variant-view.py --version 26.2 --check <dir-with-resolved-trees>
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys

BLOCK_OPEN_RE = re.compile(r"^\s*//\?\s*(if|else)\b(.*?)\{\s*$")
BLOCK_ELSE_RE = re.compile(r"^\s*(?:\*/)?//\?\}\s*else\s*\{\s*$")
BLOCK_ELSE_IF_RE = re.compile(r"^\s*(?:\*/)?//\?\}\s*else\s+if\s+(.*?)\{\s*$")
BLOCK_CLOSE_RE = re.compile(r"^\s*(?:\*/)?//\?\}?\s*$")

BASE_VERSION = "1.21.1"

REPLACEMENTS = [
    (r"\bResourceLocation\b", "Identifier", r"(?<!-)\bIdentifier\b", "ResourceLocation"),
    (r"\bGuiGraphics\b", "GuiGraphicsExtractor", r"\bGuiGraphicsExtractor\b", "GuiGraphics"),
    (r"(?<![.\w])Properties\.of\(\)", "li.cil.oc2.common.block.BlockProps.of()",
     r"li\.cil\.oc2\.common\.block\.BlockProps\.of\(\)", "Properties.of()"),
    (r"@OnlyIn\(Dist\.CLIENT\)", "@li.cil.oc2.common.util.ClientOnly",
     r"@li\.cil\.oc2\.common\.util\.ClientOnly", "@OnlyIn(Dist.CLIENT)"),
]


def parse_version(text: str) -> tuple[int, ...]:
    parts: list[int] = []
    for chunk in text.split("."):
        digits = "".join(c for c in chunk if c.isdigit())
        parts.append(int(digits) if digits else 0)
    return tuple(parts)


def compare_versions(left: str, right: str) -> int:
    left_parts = list(parse_version(left))
    right_parts = list(parse_version(right))
    length = max(len(left_parts), len(right_parts))
    left_parts += [0] * (length - len(left_parts))
    right_parts += [0] * (length - len(right_parts))
    return (left_parts > right_parts) - (left_parts < right_parts)


def evaluate_condition(condition: str, version: str) -> bool:
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


def apply_replacements(text: str, version: str) -> str:
    for forward, replacement, backward, back_replacement in REPLACEMENTS:
        if compare_versions(version, "26.1") >= 0:
            text = re.sub(forward, replacement, text)
        else:
            text = re.sub(backward, back_replacement, text)
    return text


class Branch:
    """One branch (the `if` part or its `else` part) of a conditional block."""

    def __init__(self, condition: str | None, negated: bool) -> None:
        self.condition = condition
        self.negated = negated
        self.header: str = ""
        self.body: list[str] = []
        self.closer: str = ""

    def active(self, version: str) -> bool:
        if self.condition is None:
            return True
        holds = evaluate_condition(self.condition, version)
        return not holds if self.negated else holds


def parse(text: str) -> list[Branch]:
    """Split the file into branches with their header, body and closer."""
    branches: list[Branch] = []
    stack: list[Branch] = []

    for line in text.splitlines():
        open_match = BLOCK_OPEN_RE.match(line)
        if open_match:
            condition = open_match.group(2).strip()
            branch = Branch(condition, False)
            branch.header = line
            branches.append(branch)
            stack.append(branch)
            continue
        if BLOCK_ELSE_IF_RE.match(line):
            closing = stack.pop()
            closing.closer = line
            branch = Branch(BLOCK_ELSE_IF_RE.match(line).group(1).strip(), False)
            branch.header = ""
            branches.append(branch)
            stack.append(branch)
            continue
        if BLOCK_ELSE_RE.match(line):
            closing = stack.pop()
            closing.closer = line
            branch = Branch(closing.condition, not closing.negated)
            # the `... else {` marker is emitted as the closer of the if branch
            branch.header = ""
            branches.append(branch)
            stack.append(branch)
            continue
        if BLOCK_CLOSE_RE.match(line):
            if stack:
                closing = stack.pop()
                closing.closer = line
            continue
        if stack:
            stack[-1].body.append(line)
        else:
            plain = Branch(None, False)
            plain.body.append(line)
            branches.append(plain)
    return branches


def render(text: str, version: str) -> str:
    out: list[str] = []
    for branch in parse(text):
        target_active = branch.active(version)
        base_active = branch.active(BASE_VERSION)
        if branch.header:
            out.append(branch.header)
        body = list(branch.body)
        if body and branch.condition is not None:
            commented = body[0].lstrip().startswith("/*")
            want_commented = not target_active
            if want_commented and not commented:
                body[0] = re.sub(r"^(\s*)", r"\1/*", body[0], count=1)
            elif not want_commented and commented:
                body[0] = re.sub(r"^(\s*)/\*", r"\1", body[0], count=1)
        out.extend(body)
        if branch.closer:
            closer = branch.closer
            indent = closer[: len(closer) - len(closer.lstrip())]
            marker = "//?}" if target_active else "*///?}"
            out.append(re.sub(r"^(\s*)(?:\*/)?//\?\}", r"\1" + marker.replace("\\", "\\\\"), closer, count=1))
    return "\n".join(out)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--check", help="directory with the generated tree to compare against")
    parser.add_argument("file")
    args = parser.parse_args()

    path = pathlib.Path(args.file)
    text = path.read_text(encoding="utf-8")
    resolved = apply_replacements(render(text, args.version), args.version)

    if args.check:
        root = pathlib.Path(args.check)
        repo = pathlib.Path.cwd()
        rel = path.resolve().relative_to(repo / "src/main/java")
        candidate = root / rel
        if not candidate.exists():
            print(f"MISSING {candidate}")
            return 1
        expected = candidate.read_text(encoding="utf-8")
        if expected.rstrip("\n") == resolved.rstrip("\n"):
            print(f"OK {rel}")
            return 0
        print(f"DIFF {rel}")
        expected_lines = expected.splitlines()
        resolved_lines = resolved.splitlines()
        for index in range(max(len(expected_lines), len(resolved_lines))):
            left = expected_lines[index] if index < len(expected_lines) else "<missing>"
            right = resolved_lines[index] if index < len(resolved_lines) else "<missing>"
            if left != right:
                print(f"   line {index + 1}:\n     expected: {left!r}\n     resolved: {right!r}")
                break
        return 1

    print(resolved)
    return 0


if __name__ == "__main__":
    sys.exit(main())
