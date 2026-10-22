#!/usr/bin/env python3
"""Write the shared 9x9x9 air fixture as standard Java Edition NBT."""

import gzip
from pathlib import Path
import struct


def string(value):
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def named(kind, name, payload):
    return bytes([kind]) + string(name) + payload


def integers(values):
    return struct.pack(">" + "i" * len(values), *values)


def list_tag(name, kind, entries):
    return named(9, name, bytes([kind]) + integers([len(entries)]) + b"".join(entries))


def main():
    blocks = [
        list_tag("pos", 3, [integers([coordinate]) for coordinate in (x, y, z)])
        + named(3, "state", integers([0])) + b"\0"
        for x in range(9) for y in range(9) for z in range(9)
    ]
    contents = (
        named(3, "DataVersion", integers([3955]))  # Minecraft 1.21.1; newer targets upgrade it.
        + list_tag("size", 3, [integers([9])] * 3)
        + list_tag("palette", 10, [named(8, "Name", string("minecraft:air")) + b"\0"])
        + list_tag("entities", 10, [])
        + list_tag("blocks", 10, blocks)
        + b"\0"
    )
    target = Path(__file__).resolve().parents[1] / "src/main/resources/data/oc2r/structure/empty.nbt"
    target.write_bytes(gzip.compress(named(10, "", contents), mtime=0))


if __name__ == "__main__":
    main()
