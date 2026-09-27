# Security Policy

## Supported Versions

Only the latest state of the `master` branch (the release branch) receives security fixes. There are no long-term support versions.

| Version | Supported |
| ------- | --------- |
| `master` (latest) | yes |
| anything older | no |

## Reporting a Vulnerability

Do not open a public GitHub issue for a security vulnerability.

Use [GitHub Security Advisories](https://github.com/DivByDiamond/OC2R/security/advisories/new) to report privately. This keeps the report and any discussion out of public view until a fix is ready, and lets us coordinate a CVE if one is warranted.

If you are not sure whether something is a security issue or just a bug, open a normal [GitHub issue](https://github.com/DivByDiamond/OC2R/issues) instead - most bugs are not security issues.

## Scope

OC2R embeds a full RISC-V virtual machine, a Lua-based device API, a custom in-game network stack (TCP/IP-like), and reads NBT data from world saves. Examples of what counts as a security issue here:

- Escaping the RISC-V VM or the Lua sandbox to execute code outside the emulated machine, on the host JVM
- Remote code execution or a crash triggerable by loading a crafted world save or NBT payload
- Memory-safety or denial-of-service issues in the network stack (`common/inet/*`) reachable from another player or a malicious in-game packet
- Privilege escalation between players on a shared server through any in-game device

Regular gameplay bugs, crashes without a security implication, and balance issues are not security reports - use a normal issue for those.

## Response

This project is maintained on a best-effort basis; there is no guaranteed response time. Reports are read as soon as possible and coordinated disclosure is followed: the reporter is credited (unless anonymity is requested) once a fix is released.
