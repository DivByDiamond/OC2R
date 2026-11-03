# Media Station
![Media Station](block:oc2r:media_station)

The media station combines a floppy drive and a flash/HDD programmer. Its upper bay accepts a floppy; its lower bay accepts flash memory or an HDD. A single rear bus port exposes both as independent block devices.

Insert media using the held item. Sneak-use the upper half to eject the floppy or the lower half to eject flash/HDD media. Normal use opens the firmware/image writer. Programming locks the lower bay; shut down a computer that has mounted that medium before programming it locally.

Unmount media in the guest before ejecting it. Identify guest block devices in */proc/partitions*. The old standalone drive and programmer have been removed; use the media station for both functions.
