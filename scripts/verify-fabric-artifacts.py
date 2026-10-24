#!/usr/bin/env python3
"""Check that Fabric release jars contain the matching runtime and no test code."""

import argparse
import json
from pathlib import Path
import zipfile


def verify(version, mod_version=None):
    directory = Path("fabric/versions") / version / "build/libs"
    candidates = sorted(
        path for path in directory.glob("*.jar")
        if not path.name.endswith(("-sources.jar", "-dev.jar", "-api.jar"))
    )
    if mod_version is not None:
        matching = []
        for candidate in candidates:
            with zipfile.ZipFile(candidate) as archive:
                if json.loads(archive.read("fabric.mod.json"))["version"] == mod_version:
                    matching.append(candidate)
        candidates = matching
    if len(candidates) != 1:
        raise ValueError(f"{version}: expected one release jar, found {candidates}")
    jar = candidates[0]
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        metadata = json.loads(archive.read("fabric.mod.json"))
        if metadata["id"] != "oc2r" or "${" in metadata["version"]:
            raise ValueError(f"{jar}: invalid mod identity/version")
        if metadata["depends"]["minecraft"] != f"~{version}":
            raise ValueError(f"{jar}: wrong Minecraft dependency")
        modern = not version.startswith("1.")
        java = 25 if modern else 21
        if metadata["depends"]["java"] != f">={java}":
            raise ValueError(f"{jar}: wrong Java dependency")
        if metadata["accessWidener"] not in names:
            raise ValueError(f"{jar}: missing access widener")
        required = {
            "li/cil/oc2/fabric/OC2RFabric.class",
            "li/cil/oc2/fabric/OC2RFabricClient.class",
            "li/cil/oc2/platform/Platform.class",
            "data/oc2r/file_systems/scripts.zip",
            "assets/oc2r/doc/en_us/getting_started.md",
            "assets/oc2r/doc/ru_ru/getting_started.md",
        }
        missing = required - names
        if missing:
            raise ValueError(f"{jar}: missing runtime entries {sorted(missing)}")
        forbidden = [name for name in names if
                     name.startswith(("li/cil/oc2/gametest/", "li/cil/oc2/fabric/gametest/"))
                     or name == "META-INF/neoforge.mods.toml"]
        if forbidden:
            raise ValueError(f"{jar}: test or NeoForge entries leaked: {forbidden[:5]}")
        for block in ("bus_cable", "monitor"):
            state = json.loads(archive.read(f"assets/oc2r/blockstates/{block}.json"))
            applications = list(state.get("variants", {}).values())
            applications.extend(part["apply"] for part in state.get("multipart", []))
            if not applications:
                raise ValueError(f"{jar}: missing {block} blockstate variants")
            for application in applications:
                for option in application if isinstance(application, list) else [application]:
                    if not isinstance(option, dict) or "model" not in option:
                        raise ValueError(f"{jar}: non-vanilla {block} model variant: {option}")
                    model = option["model"]
                    if model.startswith("oc2r:"):
                        target = "assets/oc2r/models/" + model.split(":", 1)[1] + ".json"
                        if target not in names:
                            raise ValueError(f"{jar}: missing model {model}")
        for name in names:
            if name.startswith("META-INF/services/li.cil.oc2.platform."):
                if b"NeoForge" in archive.read(name):
                    raise ValueError(f"{jar}: NeoForge service leaked: {name}")
        nested = {entry["file"] for entry in metadata.get("jars", [])}
        if not nested or not nested.issubset(names):
            raise ValueError(f"{jar}: missing bundled libraries")
    print(f"{version}: {jar.name}: metadata, runtime resources, libraries and isolation OK")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("versions", nargs="*", default=["1.21.1", "26.1.2", "26.2"])
    parser.add_argument("--mod-version", help="Select a specific mod version when old local jars remain")
    arguments = parser.parse_args()
    for version in arguments.versions:
        verify(version, arguments.mod_version)


if __name__ == "__main__":
    main()
