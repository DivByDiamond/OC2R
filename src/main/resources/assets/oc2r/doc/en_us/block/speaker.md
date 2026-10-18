# Speaker
![External sound](block:oc2r:speaker)

Connect the speaker through a bus interface and cable. Its RPC device name is *speaker*. Example: *local s = require("devices"):find("speaker")*, then *s:beep(880, 500)*.

*beep(frequency,duration)* and its alias *playTone* accept 20–20000 Hz and 20–5000 milliseconds. *write(data)* accepts numeric byte arrays containing 44100 Hz, mono, signed 16-bit little-endian PCM. See the [sound card](../item/sound_card.md) for the PCM format and RPC throughput limitations.

The speaker does not offer the sound card's *playSound* or *findSound* methods.
