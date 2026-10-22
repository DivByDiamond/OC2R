# SPDX-License-Identifier: MIT
"""Exercise the guest C library with bounded, fragmented raw PTY traffic."""
import json
import os
from pathlib import Path
import pty
import select
import subprocess
import tempfile
import time
import tty

ROOT = Path(__file__).resolve().parents[3]
UUID = "01234567-89ab-cdef-0123-456789abcdef"

with tempfile.TemporaryDirectory(prefix="oc2r-rpc-test-") as temporary:
    executable = str(Path(temporary) / "rpc-test")
    subprocess.run([
        "cc", "-std=c11", "-D_POSIX_C_SOURCE=200809L", "-Wall", "-Wextra", "-Werror",
        "-I", str(ROOT / "scripts/demo/guest"),
        str(ROOT / "scripts/demo/tests/rpc_host_test.c"),
        str(ROOT / "scripts/demo/guest/oc2rpc.c"), "-o", executable,
    ], check=True)
    inventory_executable = str(Path(temporary) / "inventory-test")
    subprocess.run([
        "cc", "-std=c11", "-Wall", "-Wextra", "-Werror",
        "-I", str(ROOT / "scripts/demo/guest"), str(ROOT / "scripts/demo/guest/oc2ctl.c"),
        str(ROOT / "scripts/demo/tests/inventory_precondition_mock.c"), "-o", inventory_executable,
    ], check=True)
    for empty, expected in [(False, 1), (True, 0)]:
        environment = os.environ.copy()
        environment.pop("MOCK_EMPTY_SLOT", None)
        if empty:
            environment["MOCK_EMPTY_SLOT"] = "1"
        result = subprocess.run([inventory_executable, "inventory-demo"], env=environment,
                                capture_output=True, text=True, timeout=2)
        assert result.returncode == expected, result.stdout + result.stderr
        assert "INVENTORY_TRANSFER_PRECONDITION_OK" in result.stdout
    print("INVENTORY_EMPTY_SLOT_PRECONDITION_OK")
    master, slave = pty.openpty()
    tty.setraw(slave)
    process = subprocess.Popen(
        [executable, os.ttyname(slave)], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    )
    try:
        devices = [{"deviceId": UUID, "typeNames": ["robot", "inventory"]}]
        devices += [{"deviceId": UUID, "typeNames": ["extra"]}] * 80
        deep = 0
        for _ in range(40):
            deep = [deep]
        responses = [
            {"type": "list", "data": devices},
            {"type": "result", "data": {"x": 3, "note": 'a},b["'}},
            {"type": "result"},  # Gson omits a void/null result's data.
            {"type": "error", "data": "unknown method"},
            b'{"type":"result","data":oops}',
            {"type": "result", "data": deep},
            {"type": "result", "data": "x" * 32768},
        ]
        methods = [None, "getPosition", "setSelectedSlot", "badMethod", "invalidReply", "deep", "huge"]
        receive = b""
        count = 0
        deadline = time.monotonic() + 10
        while process.poll() is None and time.monotonic() < deadline:
            if not select.select([master], [], [], 0.1)[0]:
                continue
            receive += os.read(master, 4096)
            while b"\0" in receive:
                frame, receive = receive.split(b"\0", 1)
                if not frame:
                    continue
                request = json.loads(frame)
                if count == 0:
                    assert request == {"type": "list"}
                else:
                    assert request == {
                        "type": "invoke",
                        "data": {
                            "deviceId": UUID, "name": methods[count],
                            "parameters": [0] if count == 2 else [],
                        },
                    }
                response = responses[count]
                count += 1
                wire = b""
                if count == 2:
                    # An unsolicited event must not become the call's result.
                    event = {"type": "event", "data": ["id", {"nested": "}["}]}
                    wire += b"\0" + json.dumps(event).encode() + b"\0"
                wire += b"\0" + (response if isinstance(response, bytes) else json.dumps(response, separators=(",", ":")).encode()) + b"\0"
                chunk_size = 128 if count == 7 else 3
                for offset in range(0, len(wire), chunk_size):
                    os.write(master, wire[offset:offset + chunk_size])
                    time.sleep(0.0002)
        output = process.communicate(timeout=2)[0].decode()
        print(output, end="")
        print("requests", count, "exit", process.returncode)
        assert process.returncode == 0 and count == 7
    finally:
        if process.poll() is None:
            process.kill()
            process.wait()
        os.close(master)
        os.close(slave)
