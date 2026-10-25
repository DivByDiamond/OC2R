# Fabric 26.x closeout — 2026-10-25

The implementation and original local acceptance are recorded in
[fabric-26-acceptance.md](fabric-26-acceptance.md). This follow-up separates
branch verification from publication.

## Disposable GameTest storage

CI uses `bash scripts/run-fabric-gametest.sh VERSION`. It creates a unique
run root for each invocation and Gradle appends the Minecraft version. It uses
`/dev/shm` only when writable and at least 2 GiB is available, otherwise
`RUNNER_TEMP`, `TMPDIR`, or `/tmp`. A Kubernetes container with the common small
shared-memory mount therefore uses temporary disk storage. This fallback is
correct but does not promise the same performance as a sufficiently large tmpfs.
Runner provisioning must provide adequate storage and memory for Minecraft;
this change does not resize the Kubernetes shared-memory mount.

JUnit reports remain in each version's `build/test-results/gameTest` directory.
The wrapper copies server logs and crash reports to `build/run/gameTest/logs` before cleanup so
CI artifact paths remain valid. It preserves failing/timeout exit codes and
retains the temporary world if Minecraft still holds its session lock.

The wrapper's fixture tests cover storage selection, fallback, invocation
isolation, argument forwarding, diagnostics, failure and timeout cleanup.
They run in the CI demo-scripts leg without launching Minecraft.

## Release state

GitHub release inspection on 2026-10-25 found `v0.1.1-beta.4` as the latest
release, containing the four original targets, without Fabric 26.x assets.
Both publishing token names and the existing modern/Fabric CurseForge project
variables are configured; values and tokens are not recorded here. This
establishes configuration presence, not successful uploads or moderation.

No release tag or published release is created by this closeout. The release
workflow already assembles six targets; final CI on the reviewed commit and
merge precede selecting and publishing the next release tag.

## Follow-up scope

[Upstream checks](upstream-followup-2026-10-25.md) add cable lifecycle
regressions and inspect the bundled config synchronization implementation.
Actual moving-construction and Velocity backend-switch acceptance remain
integration work; vanilla GameTests cannot establish those results.

## Prepared publication procedure

The tag build now assembles and stages exactly one primary JAR for each of the
six targets and attaches six canonically named runtime JARs to its GitHub release.
The publisher checks out an explicit existing release tag, and builds exact tag
versions even for prereleases; platform classification is independent of the
JAR version.

A release created by the build job's `GITHUB_TOKEN` does not trigger the
`release: created` publisher automatically ([GitHub trigger rules](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow)).
After the reviewed commit passes CI, is merged, and a release tag and GitHub
release exist, publishing can be explicitly dispatched, for example:

```bash
gh workflow run publish.yml --ref work -f release_tag=v0.1.1-beta.5 -f release_type=alpha
```

This is an example for the next prerelease, not a created tag or executed
publication. Select the intended release tag and the workflow branch containing
these fixes. A missing tag/release fails before artifact uploads. No publisher
was dispatched during preparation.
