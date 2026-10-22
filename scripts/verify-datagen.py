#!/usr/bin/env python3
"""Check generated 26.x assets and server data against the shipped resource baseline."""
import argparse
import json
from pathlib import Path


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def modern(value):
    if isinstance(value, list):
        return [modern(entry) for entry in value]
    if not isinstance(value, dict):
        return value
    if set(value) == {"item"}:
        return value["item"]
    if set(value) == {"tag"}:
        return "#" + value["tag"]
    return {key: modern(entry) for key, entry in value.items()
            if not ((key == "count" and entry == 1) or (key == "category" and entry == "misc"))}


def verify(root, version):
    generated = root / "versions" / version / "build/generated/datagen"
    source = root / "src/main/resources"
    client = generated / "client/assets/oc2r"
    server = generated / "server/data"
    errors = []
    models = {path.stem for path in (client / "models/item").glob("*.json")}
    definitions = {path.stem for path in (client / "items").glob("*.json")}
    expected = {path.stem for path in (source / "assets/oc2r/models/item").glob("*.json")}
    expected.discard("network_switch")  # The registered block has no item.
    if models != expected or definitions != expected:
        errors.append("Generated item models/definitions do not cover the shipped items")
    if {path.name for path in (client / "blockstates").glob("*.json")} != {
            path.name for path in (source / "assets/oc2r/blockstates").glob("*.json")}:
        errors.append("Generated blockstates do not cover the shipped blocks")
    recipes = list((server / "oc2r/recipe").glob("*.json"))
    if not recipes:
        errors.append("Server recipes are absent; run runData")
    for path in recipes:
        original = source / "data/oc2r/recipe" / path.name
        if original.exists() and modern(load(original)) != modern(load(path)):
            errors.append(f"Recipe behavior changed: {path.name}")
    for path in server.rglob("*.json"):
        relative = path.relative_to(server)
        original = source / "data" / relative
        if "tags" in relative.parts and original.exists():
            before = {json.dumps(value, sort_keys=True) for value in load(original).get("values", [])}
            after = {json.dumps(value, sort_keys=True) for value in load(path).get("values", [])}
            if before - after:
                errors.append(f"Tag lost entries: {relative}: {sorted(before - after)}")
    speaker = server / "oc2r/loot_table/blocks/speaker.json"
    if not speaker.exists() or '"oc2r:speaker"' not in speaker.read_text():
        errors.append("Speaker loot must contain its item")
    switch = server / "oc2r/loot_table/blocks/network_switch.json"
    if not switch.exists() or '"minecraft:air"' in switch.read_text():
        errors.append("The itemless switch must have a valid empty loot table")
    for message in errors:
        print(f"FAIL {version}: {message}")
    if not errors:
        print(f"PASS {version}: {len(models)} models/definitions, blockstates, "
              f"{len(recipes)} recipe behaviors, tag preservation and loot")
    return not errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("versions", nargs="*", default=["26.1.2", "26.2"])
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    results = [verify(root, version) for version in args.versions]
    raise SystemExit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
