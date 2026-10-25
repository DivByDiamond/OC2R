#!/usr/bin/env python3
"""Exercise disposable Fabric GameTest storage without starting Gradle or Minecraft."""

import fcntl
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


RUNNER = Path(__file__).with_name("run-fabric-gametest.sh")


class FabricGameTestRunnerTest(unittest.TestCase):
    def setUp(self):
        self.scratch = tempfile.TemporaryDirectory(prefix="oc2r-runner-test.")
        self.addCleanup(self.scratch.cleanup)
        self.root = Path(self.scratch.name)
        (self.root / "scripts").mkdir()
        shutil.copy2(RUNNER, self.root / "scripts" / RUNNER.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.fallback = self.root / "runner temp"
        self.fallback.mkdir()
        self.calls = self.root / "calls.jsonl"
        self.env = os.environ.copy()
        self.env.update(
            PATH=f"{self.bin}{os.pathsep}{self.env['PATH']}",
            RUNNER_TEMP=str(self.fallback),
            TEST_CALLS=str(self.calls),
            TEST_AVAILABLE="1",
            TEST_DF_STATUS="0",
            TEST_GRADLE_STATUS="0",
        )
        self.executable(self.bin / "df", "#!/bin/sh\nprintf 'Filesystem 1024-blocks Used Available Capacity Mounted on\\n'\nprintf 'tmpfs 8388608 0 %s 0%% /dev/shm\\n' \"$TEST_AVAILABLE\"\nexit \"$TEST_DF_STATUS\"\n")
        self.executable(self.root / "gradlew", """#!/usr/bin/env python3
import json, os, pathlib, sys
arguments = sys.argv[1:]
root = pathlib.Path(next(a.split('=', 1)[1] for a in arguments if a.startswith('-Pfabric.gameTestRunDir=')))
version = arguments[0].split(':')[2]
logs = root / version / 'logs'
logs.mkdir(parents=True)
(logs / 'latest.log').write_text('fixture server log\\n')
crashes = root / version / 'crash-reports'
crashes.mkdir()
(crashes / 'fixture.txt').write_text('fixture crash report\\n')
(logs / 'nested').mkdir()
(logs / 'nested' / 'debug.log').write_text('fixture nested log\\n')
with open(os.environ['TEST_CALLS'], 'a') as output:
    output.write(json.dumps({'arguments': arguments, 'root': str(root)}) + '\\n')
sys.exit(int(os.environ['TEST_GRADLE_STATUS']))
""")

    @staticmethod
    def executable(path, content):
        path.write_text(content)
        path.chmod(0o755)

    def run_runner(self, version="26.1.2", *arguments):
        return subprocess.run(
            ["bash", str(self.root / "scripts" / RUNNER.name), version, *arguments],
            cwd=self.root,
            env=self.env,
            text=True,
            capture_output=True,
            timeout=15,
        )

    def read_calls(self):
        return [json.loads(line) for line in self.calls.read_text().splitlines()]

    def assert_log_copy_and_cleanup(self, call, version="26.1.2"):
        self.assertFalse(Path(call["root"]).exists(), "disposable world was not removed")
        logs = self.root / "fabric" / "versions" / version / "build/run/gameTest/logs"
        self.assertEqual((logs / "latest.log").read_text(), "fixture server log\n")
        self.assertEqual((logs / "nested/debug.log").read_text(), "fixture nested log\n")
        self.assertEqual((logs.parent / "crash-reports/fixture.txt").read_text(), "fixture crash report\n")

    def test_small_shm_uses_runner_temp_and_preserves_arguments_logs(self):
        result = self.run_runner("26.1.2", "--console=plain", "-Pfixture=value with spaces")
        self.assertEqual(result.returncode, 0, result.stderr)
        call = self.read_calls()[0]
        self.assertEqual(Path(call["root"]).parent, self.fallback)
        self.assertIn("-Pfixture=value with spaces", call["arguments"])
        self.assertEqual(call["arguments"][0], ":fabric:26.1.2:runGameTest")
        self.assert_log_copy_and_cleanup(call)

    @unittest.skipUnless(os.access("/dev/shm", os.W_OK), "writable /dev/shm required")
    def test_sufficient_shm_is_selected(self):
        self.env["TEST_AVAILABLE"] = "2097152"
        result = self.run_runner()
        self.assertEqual(result.returncode, 0, result.stderr)
        call = self.read_calls()[0]
        self.assertEqual(Path(call["root"]).parent, Path("/dev/shm"))
        self.assert_log_copy_and_cleanup(call)

    def test_df_failure_falls_back(self):
        self.env["TEST_DF_STATUS"] = "1"
        result = self.run_runner()
        self.assertEqual(result.returncode, 0, result.stderr)
        call = self.read_calls()[0]
        self.assertEqual(Path(call["root"]).parent, self.fallback)
        self.assert_log_copy_and_cleanup(call)

    def test_missing_runner_temp_uses_tmpdir(self):
        self.env.pop("RUNNER_TEMP")
        self.env["TMPDIR"] = str(self.fallback)
        result = self.run_runner()
        self.assertEqual(result.returncode, 0, result.stderr)
        call = self.read_calls()[0]
        self.assertEqual(Path(call["root"]).parent, self.fallback)
        self.assert_log_copy_and_cleanup(call)

    def test_timeout_status_is_propagated_and_world_removed(self):
        self.executable(self.bin / "timeout", "#!/bin/sh\nexit 124\n")
        result = self.run_runner()
        self.assertEqual(result.returncode, 124, result.stderr)
        self.assertFalse(self.calls.exists())
        self.assertEqual(list(self.fallback.iterdir()), [])

    def test_invalid_df_output_falls_back(self):
        self.env["TEST_AVAILABLE"] = "unknown"
        result = self.run_runner()
        self.assertEqual(result.returncode, 0, result.stderr)
        call = self.read_calls()[0]
        self.assertEqual(Path(call["root"]).parent, self.fallback)
        self.assert_log_copy_and_cleanup(call)

    def test_gradle_failure_preserves_status_logs_and_cleanup(self):
        self.env["TEST_GRADLE_STATUS"] = "23"
        result = self.run_runner()
        self.assertEqual(result.returncode, 23, result.stderr)
        self.assert_log_copy_and_cleanup(self.read_calls()[0])

    def test_repeated_runs_get_unique_roots(self):
        for _ in range(2):
            result = self.run_runner("1.21.1")
            self.assertEqual(result.returncode, 0, result.stderr)
        calls = self.read_calls()
        self.assertNotEqual(calls[0]["root"], calls[1]["root"])
        for call in calls:
            self.assert_log_copy_and_cleanup(call, "1.21.1")

    def test_unsupported_version_never_starts_gradle(self):
        result = self.run_runner("unknown")
        self.assertEqual(result.returncode, 2, result.stderr)
        self.assertFalse(self.calls.exists())
        self.assertEqual(list(self.fallback.iterdir()), [])

    def test_live_world_lock_prevents_timeout_cleanup(self):
        self.assert_live_lock_retained(124)

    def test_live_world_lock_turns_success_into_failure(self):
        self.assert_live_lock_retained(0)

    def assert_live_lock_retained(self, status):
        run_root = self.fallback / "locked-world"
        lock = run_root / "26.1.2/world/session.lock"
        lock.parent.mkdir(parents=True)
        lock.write_bytes(b"lock")
        # mktemp's fake path already exists because the parent must hold its lock.
        self.executable(self.bin / "mktemp", f"#!/bin/sh\nprintf '%s\\n' '{run_root}'\n")
        self.executable(self.bin / "timeout", f"#!/bin/sh\nexit {status}\n")
        with lock.open("r+b") as held:
            fcntl.lockf(held, fcntl.LOCK_EX | fcntl.LOCK_NB)
            result = self.run_runner()
            self.assertEqual(result.returncode, status or 1, result.stderr)
            self.assertTrue(run_root.is_dir())
            self.assertIn("retaining Fabric GameTest world", result.stderr)
        shutil.rmtree(run_root)

    def test_unwritable_log_destination_still_cleans_world(self):
        # A file where mkdir expects a directory fails reliably even when run as root.
        (self.root / "fabric").write_text("blocked directory")
        result = self.run_runner()
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(Path(self.read_calls()[0]["root"]).exists())


if __name__ == "__main__":
    unittest.main(verbosity=2)
