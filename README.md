# OpenComputers II: Reimagined

[Searching for an Artist](#artists)

*OpenComputers II: Reimagined* is a Minecraft mod adding virtual computers to the game. It is a fork of the mod originally made by [Sangar (fnuecke)], this fork is maintained North Western Development. These computers run a virtual machine emulating a 64-Bit RISC-V architecture capable of booting Linux. On top of this, a high-level Lua API is provided to communicate with various devices in the game world. This enables adding virtual devices using a simple, Java-friendly API, without having to implement actual kernel drivers.

The original mod was a successor to [OpenComputers]. At least in spirit. While many of the implementation details have changed quite dramatically, the core concepts of customizable hardware, persistence and sand-boxing are shared.

The underlying VM, which is written entirely in Java, is called [Sedna] and was written by [Sangar (fnuecke)] as well.

## Artists

We are looking for artists to assist with models and textures going forward. Artists will be entitled to a negotiated percentage of CurseForge rewards or any other donation platforms that may be employed. Artists will also receive special credits throughout the mod and it's repos.

## Why the Fork?

*OpenComputers II: Reimagined* aims to fix bugs with OC2 while adding new features and attempting to keep up with the latest popular versions of Minecraft. We did attempt to reach [Sangar (fnuecke)] on Twitter but got no response, so we've decided to continue with the fork and release separately on Curse under a new name.
The name change is simply an attempt to ensure it is well known that [Sangar (fnuecke)] is not responsible for this version, so they're not blasted with issues from it. We will be doing everything we can to ensure [Sangar (fnuecke)] does still get the credit they deserve for the mod though.

## Gameplay Documentation
For documentation on how the to get computers up and running, and how to use them, see the [documentation]. It is available as a manual item in the game.

## Development Documentation
The original section that was found here is preserved below, however it should be taken with a grain of salt as the current team has a ways to go in being 100% up to speed with where the device APIs are at the moment. So everything below is subject to change, though it is likely to remain mostly the same.

~~While the mod isn't quite yet ready for release due to some remaining technical and usability issues, the API should be mostly stable at this point. For most people the high level device API will be sufficient, and is much more accessible. It centers around the [`RPCDevice`][RPC Device]. For a sample block implementation, see the [redstone interface]. For a sample item implementation, see the [sound card]. If you wish to dive deeper, and provide emulated hardware that requires a Linux driver, this centers around the [`VMDevice`][VM Device]. For a sample block implementation, see the [disk drive]. For a sample item implementation, see the [network card].~~

[OpenComputers]: https://github.com/MightyPirates/OpenComputers
[RPC Device]: src/main/java/li/cil/oc2/api/bus/device/rpc/RPCDevice.java
[redstone interface]: src/main/java/li/cil/oc2/common/blockentity/RedstoneInterfaceBlockEntity.java
[sound card]: src/main/java/li/cil/oc2/common/bus/device/rpc/item/SoundCardItemDevice.java
[VM Device]: src/main/java/li/cil/oc2/api/bus/device/vm/VMDevice.java
[disk drive]: src/main/java/li/cil/oc2/common/blockentity/DiskDriveBlockEntity.java
[network card]: src/main/java/li/cil/oc2/common/bus/device/vm/item/NetworkInterfaceCardDevice.java
[documentation]: src/main/resources/assets/oc2r/doc/en_us/index.md
[GithubPackagesGradle]: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry
[Sangar (fnuecke)]: https://github.com/fnuecke
[Sedna]: https://github.com/fnuecke/sedna
