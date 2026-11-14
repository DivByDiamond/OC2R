#!/usr/bin/env python3
"""Switch off TX checksum offload and the offloads that depend on it (what `ethtool -K dev tx off` does)."""
import array
import fcntl
import socket
import struct
import sys

SIOCETHTOOL = 0x8946
ETHTOOL_STXCSUM = 0x17
ETHTOOL_SSG = 0x1B
ETHTOOL_STSO = 0x1E
ETHTOOL_SGSO = 0x24
ETHTOOL_SGRO = 0x2C


def set_flag(sock, device, command, value):
    request = array.array("B", struct.pack("II", command, value))
    address, _ = request.buffer_info()
    ifreq = struct.pack("16sP", device.encode(), address)
    try:
        fcntl.ioctl(sock, SIOCETHTOOL, ifreq)
    except OSError as failure:
        print(f"tx_offload_off: {device} command {command:#x}: {failure}", file=sys.stderr)


with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as control:
    for name in sys.argv[1:]:
        for command in (ETHTOOL_STXCSUM, ETHTOOL_SSG, ETHTOOL_STSO, ETHTOOL_SGSO, ETHTOOL_SGRO):
            set_flag(control, name, command, 0)
