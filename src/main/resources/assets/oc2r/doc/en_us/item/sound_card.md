# Sound Card
![Less sound of silence](item:oc2r:sound_card)

The sound card enables playing back various sound effects from its vast library of life-like samples. Due to internal engineering constraints, playback of sequential effects requires a small pause. Attempts to play back additional effects in this time window will have no effect.

This is a high level device. It must be controlled using the high level device API. The default Linux distribution offers Lua libraries for this API. For example:  
*local d = require("devices")*  
*local s = d:find("sound")*  
*s:playSound("entity.creeper.primed")*

## API
Device name: *sound*

### Methods
*playSound(name:string[,volume:float,pitch:float])* plays back the sound effect with the specified name.
- *name* is the name of the effect to play.
- *volume* is the volume at which to play the effect in the range from *0* to *1*, with *1* being the normal volume. Optional, defaulting to *1*.
- *pitch* is the pitch at which to play the effect in the range from *0.5* to *2*, with *1* being the normal pitch. Optional, defaulting to *1*.
- Throws if the specified name is invalid.

*findSound(name:string):table* returns a list of available sound effects matching the given name. Note that the number of results is limited, so overly generic queries will result in truncated results.
- *name* the name query to search for.
- Returns a list of sound effect names matching the query string.

## Tone and PCM

*beep(frequency:number,duration:number)* plays a tone from 20 to 20000 Hz for 20 to 5000 milliseconds. *playTone* is an alias. Example: *s:beep(880, 500)*.

*write(data:table)* accepts a numeric byte array of signed 16-bit little-endian mono PCM at 44100 Hz. Each sample occupies two bytes. RPC uses JSON arrays, not Lua binary strings. Send small chunks and pace them; the card enforces a configurable byte budget (131072 bytes/second by default). RPC throughput can be much lower than real-time audio. This is not an MP3/WAV decoder; remove container headers and supply decoded PCM.

*findSound* returns at most 25 matching names. The effect cooldown applies to *playSound*. For an external bus device with tone/PCM support, see the [speaker](../block/speaker.md).
