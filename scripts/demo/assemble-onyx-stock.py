#!/usr/bin/env python3
"""Assemble a fresh stock root while retaining missing packaged /bin tools."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import struct
import subprocess

CAPACITY = 16 * 1024 * 1024
TOOLS = ('netcfg', 'udpdemo', 'tcpdemo', 'tcpserve', 'udpserve', 'tcpstream', 'oc2ctl', 'rshd')
# Tools that init starts from /service; they stay disabled until /etc/init/<name>.enabled exists.
SERVICES = ('rshd',)


class OnyxImage:
    def __init__(self, path):
        self.data = Path(path).read_bytes()
        magic, version, self.block, _, self.count, table = struct.unpack_from('<6I', self.data)
        if magic != 0x32594E4F or version != 2 or self.block != 4096:
            raise ValueError('expected OnyxFS v2 with 4096-byte blocks')
        self.table = table * self.block
        self.root = struct.unpack_from("<I", self.data, 32)[0]

    def inode(self, number):
        if not 1 <= number <= self.count:
            raise ValueError('inode outside filesystem')
        offset = self.table + (number - 1) * 128
        mode = struct.unpack_from('<I', self.data, offset)[0]
        size = struct.unpack_from('<Q', self.data, offset + 8)[0]
        flags = struct.unpack_from('<I', self.data, offset + 68)[0]
        if flags & 1:
            raise ValueError('compressed stock files are unsupported')
        blocks = list(struct.unpack_from('<10I', self.data, offset + 16))
        if mode & 0o170000 == 0o040000 and size == 0:
            size = sum(bool(block) for block in blocks) * self.block
        indirect, double = struct.unpack_from('<2I', self.data, offset + 96)
        if mode & 0o170000 == 0o040000 and indirect:
            raise ValueError('indirect directories are unsupported in stock image')
        if double:
            raise ValueError('double indirect files are unsupported in stock image')
        if indirect:
            blocks.extend(struct.unpack_from('<1024I', self.data, indirect * self.block))
        content = bytearray()
        for block in blocks[:(size + self.block - 1) // self.block]:
            if not block or (block + 1) * self.block > len(self.data):
                raise ValueError('file block outside filesystem')
            content.extend(self.data[block * self.block:(block + 1) * self.block])
        if len(content) < size:
            raise ValueError('file exceeds supported block pointers')
        return mode, bytes(content[:size])

    def directory(self, number):
        mode, content = self.inode(number)
        if mode & 0o170000 != 0o040000:
            raise ValueError('expected directory')
        result = {}
        for start in range(0, len(content), self.block):
            for offset in range(start, min(start + self.block, len(content)) - 39, 40):
                name = content[offset:offset + 32].split(b'\0', 1)[0].decode('ascii')
                inode = struct.unpack_from('<I', content, offset + 32)[0]
                if name and name not in ('.', '..'):
                    if '/' in name or not inode:
                        raise ValueError('invalid directory entry')
                    result[name] = inode
        return result

    def tree(self, target):
        """Write every directory and regular file of the image under target; return manifest lines."""
        lines = []

        def walk(number, path):
            for name, inode in sorted(self.directory(number).items()):
                mode, content = self.inode(inode)
                guest = f'{path}/{name}'
                if mode & 0o170000 == 0o040000:
                    lines.append(f'dir {guest}')
                    walk(inode, guest)
                elif mode & 0o170000 == 0o100000:
                    host = target / guest.strip('/').replace('/', '__')
                    host.write_bytes(content)
                    lines.append(f'file {host} {guest}')

        walk(self.root, '')
        return lines

    def binaries(self, folder='bin'):
        root = self.directory(self.root)
        return {name: self.inode(inode)[1]
                for name, inode in self.directory(root[folder]).items()
                if self.inode(inode)[0] & 0o170000 == 0o100000}


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--fresh-manifest', type=Path,
                        help='manifest written by OnyxOS scripts/build-oc2r.sh')
    source.add_argument('--reuse-previous-root', action='store_true',
                        help='start from every directory and file of --previous-root, not from a fresh build')
    parser.add_argument('--previous-root', type=Path, required=True)
    parser.add_argument('--kernel', type=Path, required=True)
    parser.add_argument('--tools', type=Path, required=True)
    parser.add_argument('--mkimage', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    if args.reuse_previous_root:
        reused = output / 'previous-root'
        reused.mkdir(exist_ok=True)
        lines = OnyxImage(args.previous_root).tree(reused)
    else:
        lines = args.fresh_manifest.read_text().splitlines()
    binaries = {}
    for line in lines:
        fields = line.split()
        if not fields or fields[0].startswith("#"):
            continue
        if fields[0] == 'file':
            if len(fields) != 3:
                raise ValueError(f'invalid file manifest entry: {line}')
            if fields[2].startswith('/bin/'):
                binaries[fields[2][5:]] = Path(fields[1])
        elif fields[0] != 'dir' or len(fields) != 2:
            raise ValueError(f'invalid manifest entry: {line}')
    previous = OnyxImage(args.previous_root).binaries()
    retained = sorted(set(previous) - set(binaries) - set(TOOLS))
    legacy = output / 'retained-bin'
    legacy.mkdir(exist_ok=True)
    for name in retained:
        path = legacy / name
        path.write_bytes(previous[name])
        binaries[name] = path
        lines.append(f'file {path} /bin/{name}')
    # Replace any fresh entries with rebuilt OC2R transport tools.
    lines = [line for line in lines if not (
        line.startswith('file ') and line.split()[-1] in {f'/bin/{tool}' for tool in TOOLS})]
    for name in TOOLS:
        path = (args.tools / (name + '.onx')).resolve()
        if not path.is_file():
            raise ValueError(f'missing rebuilt tool: {path}')
        binaries[name] = path
        lines.append(f'file {path} /bin/{name}')
    lines = [line for line in lines if not (
        line.startswith('file ') and line.split()[-1] in {f'/service/{name}' for name in SERVICES})]
    for name in SERVICES:
        lines.append(f'file {binaries[name]} /service/{name}')
    manifest = output / 'manifest.txt'
    manifest.write_text('\n'.join(lines) + '\n')
    image = output / 'onyxfs.img'
    subprocess.run([str(args.mkimage.resolve()), str(manifest), str(image)], check=True)
    if image.stat().st_size > CAPACITY:
        raise ValueError(f'root exceeds 16 MiB: {image.stat().st_size}')
    payload_size = image.stat().st_size
    with image.open('ab') as target:
        target.truncate(CAPACITY)
    packed = OnyxImage(image).binaries()
    for name, source in binaries.items():
        if packed.get(name) != source.read_bytes():
            raise ValueError(f'packed binary mismatch: {name}')
    if not set(previous) <= set(packed):
        raise ValueError('previous packaged /bin tool was lost')
    packed_services = OnyxImage(image).binaries('service')
    for name in SERVICES:
        if packed_services.get(name) != binaries[name].read_bytes():
            raise ValueError(f'packed service mismatch: {name}')
    shutil.copyfile(args.kernel, output / 'onyx-kernel.bin')
    report = {'retained_bin': retained, 'bin': sorted(packed), 'service': sorted(packed_services), 'payload_bytes': payload_size,
              'capacity_bytes': CAPACITY, 'previous_root_sha256': digest(args.previous_root),
              'kernel_sha256': digest(output / 'onyx-kernel.bin'), 'root_sha256': digest(image)}
    (output / 'assembly.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
