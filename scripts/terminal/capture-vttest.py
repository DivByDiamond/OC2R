#!/usr/bin/env python3
"""Capture pinned, unmodified vttest pages over a PTY; author goldens with libvterm."""
import argparse
import fcntl
import hashlib
import os
from pathlib import Path
import pty
import select
import struct
import subprocess
import tempfile
import termios

REVISION = 'd37932dc8a7e5dab702ba34020bdbb12cdf12858'
ROOT = Path(__file__).resolve().parents[2]


def run(*args, **kwargs):
    return subprocess.check_output(args, **kwargs).decode().strip()


def capture(binary, choices):
    master, slave = pty.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack('HHHH', 24, 80, 0, 0))
    child = subprocess.Popen([str(binary), '24x80.132'], stdin=slave, stdout=slave, stderr=slave,
                             env={**os.environ, 'TERM': 'vt420'})
    os.close(slave)

    def read():
        data = b''
        while select.select([master], [], [], 1)[0]:
            data += os.read(master, 65536)
            if len(data) > 2_000_000:
                raise RuntimeError('unexpected unbounded vttest output')
        return data

    try:
        initial = read()
        if initial != b'\x1b[0c':
            raise RuntimeError(f'unexpected startup query: {initial!r}')
        os.write(master, b'\x1b[?64;1;2;6;8;9c')
        if b'Enter choice number' not in read():
            raise RuntimeError('main menu did not appear')
        for choice in choices:
            os.write(master, choice.encode() + b'\n')
            page = read()
        if b'Push <RETURN>' not in page:
            raise RuntimeError(f'not a held page: {page[-300:]!r}')
        # Drop the echoed menu selection; retain the page's own clear/reset sequences.
        start = page.find(b'\x1b[2J')
        if start < 0:
            raise RuntimeError('page has no initial display clear')
        return page[start:]
    finally:
        child.kill()
        child.wait()
        os.close(master)


def author(oracle, target, width):
    rows = [[None] * width for _ in range(24)]
    for line in run(str(oracle), str(width), '24', str(target / 'stream.bin')).splitlines():
        y, x, cp, style, attr, fg, bg = line.split('\t')
        rows[int(y)][int(x)] = (int(cp), style, attr, fg, bg)
    for name, column in [('styles.expected', 1), ('foreground.expected', 3), ('background.expected', 4)]:
        (target / name).write_text('\n'.join('|'.join(cell[column] for cell in row) for row in rows) + '\n')
    (target / 'screen.expected').write_text('\n'.join(''.join(chr(c[0]) for c in row).rstrip() for row in rows) + '\n')
    (target / 'lineattrs.expected').write_text('\n'.join(row[0][2] for row in rows) + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / 'build/reports/vttest-capture')
    args = parser.parse_args()
    if run('pkg-config', '--modversion', 'vterm') != '0.3.3':
        raise RuntimeError('golden authoring requires the reviewed libvterm 0.3.3 oracle')
    source = ROOT / 'ref/vttest'
    if run('git', '-C', str(source), 'rev-parse', 'HEAD') != REVISION:
        raise RuntimeError(f'vttest must be pinned to {REVISION}')
    if run('git', '-C', str(source), 'status', '--porcelain', '--untracked-files=no'):
        raise RuntimeError('tracked upstream sources have changes')
    with tempfile.TemporaryDirectory(prefix='oc2r-vttest-') as temporary:
        build = Path(temporary)
        env = {**os.environ, 'CFLAGS': '-O2 -std=gnu89'}
        subprocess.run([str(source / 'configure'), '--srcdir=' + str(source)], cwd=build, env=env,
                       stdout=subprocess.DEVNULL, check=True)
        subprocess.run(['make', '-j4'], cwd=build, stdout=subprocess.DEVNULL, check=True)
        oracle = build / 'reference-grid'
        subprocess.run(['cc', '-Wall', '-Wextra', '-o', str(oracle),
                        str(ROOT / 'scripts/terminal/reference-grid.c'), '-lvterm'], check=True)
        pages = [('upstream-cursor-80', 80, ['1'], 'main.c:tst_movements, first hold'),
                 ('upstream-double-80', 80, ['4'], 'main.c:tst_doublesize, first hold'),
                 ('upstream-double-132', 132, ['4', '', ''], 'main.c:tst_doublesize, third hold'),
                 ('upstream-colors-80', 80, ['11', '6', '2'], 'color.c:show_test_pattern, first hold')]
        for name, width, choices, page in pages:
            target = args.output / name
            target.mkdir(parents=True, exist_ok=True)
            stream = capture(build / 'vttest', choices)
            (target / 'stream.bin').write_bytes(stream)
            (target / 'page.properties').write_text(
                f'id={name}\ngeometry={width}x24\nstatus=pass\nsource.revision={REVISION}\n'
                f'source.url=https://github.com/hharte/vttest/tree/{REVISION}\n'
                f'source.page={page}\nsource.input={choices!r}\n'
                f'stream.sha256={hashlib.sha256(stream).hexdigest()}\n'
                f'oracle=libvterm {run("pkg-config", "--modversion", "vterm")}\n'
                'oracle.command=scripts/terminal/reference-grid.c\n')
            author(oracle, target, width)
            print(name, len(stream), hashlib.sha256(stream).hexdigest())


if __name__ == '__main__':
    main()
