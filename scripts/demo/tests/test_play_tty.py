#!/usr/bin/env python3
"""Runs play.lua in a real pseudo terminal (real stty, real /dev/tty reads) with a stub of the
"devices" module that logs what the player sends. Checks the keys without Enter, the 0.1 s
non-blocking read, and that the terminal settings are restored on exit and on an error.

Needs lua5.4 (or lua) and stty on the host:  python3 scripts/demo/tests/test_play_tty.py
"""
import os
import pty
import select
import shutil
import sys
import tempfile
import termios
import time

HERE = os.path.dirname(os.path.abspath(__file__))
PLAY = os.path.join(HERE, "..", "..", "..", "src", "main", "scripts", "bin", "play.lua")

STUB = r'''
local log = assert(io.open(os.getenv("PLAY_TEST_LOG"), "a"))
log:setvbuf("line")
local writes = 0
local device = {}
function device:getBufferedMs() return 0 end
function device:writeDfpwm(data, rate)
  writes = writes + 1
  log:write(string.format("W %d %d %d\n", #data, rate, data[1]))
  if os.getenv("PLAY_TEST_FAIL_AT") and writes == tonumber(os.getenv("PLAY_TEST_FAIL_AT")) then
    error("stub bus failure")
  end
  return true
end
return { find = function(_, name) if name == "speaker" then return device end end }
'''

failures = 0


def check(cond, message):
    global failures
    if not cond:
        failures += 1
        print("FAIL: " + message, file=sys.stderr)


def lua_binary():
    for name in ("lua5.4", "lua"):
        if shutil.which(name):
            return shutil.which(name)
    sys.exit("test_play_tty.py: no lua interpreter found")


class Session:
    def __init__(self, workdir, args, env_extra=None):
        self.log = os.path.join(workdir, "log")
        open(self.log, "w").close()
        env = dict(os.environ, LUA_PATH=workdir + "/?.lua;;", PLAY_TEST_LOG=self.log, TERM="xterm")
        env.update(env_extra or {})
        self.out = b""
        pid, fd = pty.fork()
        if pid == 0:
            os.execve(lua_binary(), [lua_binary(), PLAY] + args, env)
        self.pid, self.fd = pid, fd
        self.initial = termios.tcgetattr(fd)

    def pump(self, seconds):
        end = time.time() + seconds
        while time.time() < end:
            r, _, _ = select.select([self.fd], [], [], 0.05)
            if r:
                try:
                    data = os.read(self.fd, 65536)
                except OSError:
                    return
                if not data:
                    return
                self.out += data

    def send(self, data):
        os.write(self.fd, data)

    def writes(self):
        with open(self.log) as f:
            return [line.split() for line in f if line.startswith("W")]

    def finish(self, timeout=5):
        end = time.time() + timeout
        while time.time() < end:
            self.pump(0.1)
            done, status = os.waitpid(self.pid, os.WNOHANG)
            if done:
                self.pump(0.1)
                return os.waitstatus_to_exitcode(status)
        os.kill(self.pid, 9)
        os.waitpid(self.pid, 0)
        return None

    def termios_now(self):
        return termios.tcgetattr(self.fd)


def main():
    if not shutil.which("stty"):
        sys.exit("test_play_tty.py: stty not found")
    workdir = tempfile.mkdtemp(prefix="playtty")
    try:
        with open(os.path.join(workdir, "devices.lua"), "w") as f:
            f.write(STUB)
        song = os.path.join(workdir, "song.dfpwm")
        with open(song, "wb") as f:
            f.write(bytes([0x55] * 400000))  # 66 s at 48000 Hz: still playing when the keys arrive

        # Pause, seek, restart and quit by single key presses, no Enter.
        s = Session(workdir, [song])
        s.pump(1.0)
        raw = s.termios_now()
        check(not (raw[3] & termios.ICANON) and not (raw[3] & termios.ECHO), "terminal is raw without echo while playing")
        check(b"song.dfpwm" in s.out and b"Playing" in s.out, "screen shows the file name and Playing")
        n0 = len(s.writes())
        check(n0 > 0, "chunks are sent while playing")
        s.send(b" ")
        s.pump(0.6)
        n1 = len(s.writes())
        s.pump(1.0)
        n2 = len(s.writes())
        check(n1 == n2, "pause stops sending in the real terminal (%d vs %d)" % (n1, n2))
        check(b"Paused" in s.out, "screen shows Paused")
        s.send(b" ")
        s.pump(1.0)
        check(len(s.writes()) > n2, "sending resumes after the second space")
        before = len(s.out)
        s.send(b"\x1b[C")  # Right
        s.pump(0.6)
        s.send(b"\x1b[D")  # Left
        s.pump(0.6)
        s.send(b"r")
        s.pump(0.6)
        check(len(s.out) > before, "the screen keeps updating after the seek and restart keys")
        s.send(b"q")
        code = s.finish()
        check(code == 0, "exit code 0 after q (got %r)" % (code,))
        after = s.termios_now()
        check(after[3] == s.initial[3] and after[0] == s.initial[0] and after[1] == s.initial[1],
              "terminal settings restored after q")
        check(b"\x1b[?25h" in s.out and b"\x1b[?1049l" in s.out, "cursor shown and screen restored after q")

        # An error inside the player (the stub raises on the third write).
        s = Session(workdir, [song], {"PLAY_TEST_FAIL_AT": "3"})
        code = s.finish()
        check(code == 1, "exit code 1 after an error (got %r)" % (code,))
        after = s.termios_now()
        check(after[3] == s.initial[3] and after[0] == s.initial[0], "terminal settings restored after an error")
        check(b"stub bus failure" in s.out, "the error is printed")

        # Ctrl-C in raw mode is a key, not a signal, and also stops the player.
        s = Session(workdir, [song])
        s.pump(0.8)
        s.send(b"\x03")
        code = s.finish()
        check(code == 0, "Ctrl-C quits cleanly (got %r)" % (code,))
        check(s.termios_now()[3] == s.initial[3], "terminal settings restored after Ctrl-C")

        # --simple needs no terminal interface.
        s = Session(workdir, ["--simple", song])
        code = s.finish(20)
        check(code == 0 and len(s.writes()) == 400000 // 4096 + 1, "--simple sends the whole file (%d writes)" % len(s.writes()))
    finally:
        shutil.rmtree(workdir, ignore_errors=True)
    if failures:
        sys.exit("test_play_tty.py: %d checks failed" % failures)
    print("test_play_tty.py: ok")


main()
