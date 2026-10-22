# Hard Drive
![Data storage](item:oc2r:hard_drive_large)

Hard drives add persistent storage to [computers](../block/computer.md) and [robots](robot.md). The default Linux already has a built-in writable root disk: an extra hard drive is optional. Its overlay is saved with the VM, independently of removable disks.

Extra disks appear as Linux block devices such as */dev/vdb*. Device letters depend on the attached devices: inspect */proc/partitions* before formatting. Format a new data disk, create a mount directory and mount it using the tools described for the [disk drive](../block/disk_drive.md). Formatting destroys existing data. Never format the built-in root disk to prepare an extra data disk.

The OnyxOS disk recipe uses the medium hard drive; it is separate from the standard Linux boot path. Custom OS images can require a different disk layout.

Shut down before installing or removing a hard drive. Preserve the original item to preserve its data.
