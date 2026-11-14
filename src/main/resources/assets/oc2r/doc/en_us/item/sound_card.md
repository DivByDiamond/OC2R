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

## Playing music

Raw PCM needs 88200 numbers per second through RPC, which is far more than a guest can send in real time. Music is therefore played as **DFPWM**, a 1-bit-per-sample format that needs 6 KB/s (the format ComputerCraft speakers use).

*writeDfpwm(data:table[,sampleRate:number]):boolean* decodes one chunk and queues it. *data* is a numeric byte array of at most 8192 bytes, least significant bit first. *sampleRate* is the rate the file was recorded at, from 8000 to 48000, default 48000; the speaker resamples to its own 44100 Hz. Decoder state carries over from chunk to chunk, so cut a file into chunks and send them in order.

The call returns *false*, and consumes nothing, when about two seconds of audio are already queued. *getBufferedMs():number* returns how much audio is queued ahead of the playback position. Wait and send the same chunk again, for example `while not s:writeDfpwm(chunk) do os.execute("sleep 0.2") end`. The raw PCM *write* follows the same rule and also returns *true* or *false*. After about a second of silence the next write starts a new stream.

Convert any audio file on your PC with ffmpeg, then bring it into the guest with the [file import/export card](../item/file_import_export_card.md) or a floppy:

*ffmpeg -i song.mp3 -ac 1 -ar 48000 -f dfpwm song.dfpwm*

The default Linux image has a player: *play.lua song.dfpwm* (or *play.lua -r 32000 song.dfpwm* for a file recorded at another rate). The bus accepts a guest message of at most 4096 bytes and a byte costs up to four characters of JSON, so send about 900 bytes per call even though the method itself takes up to 8192.

OnyxOS has its own player, *play*, with a terminal screen showing the file name, a progress bar, elapsed and total time and the play/pause state. *play song.dfpwm* or *play -r 32000 song.dfpwm* plays a file; *play* alone lists the *.dfpwm* files of the current directory (or of */usr/share/music* when there are none) and lets you pick one with the arrow keys and Enter. Keys: Space or *p* pauses and resumes, *r* starts over, *b* returns to the list, *q* or Ctrl-C quits. It keeps about a second of audio queued, so a pause or a restart takes up to a second to be heard. The OnyxOS image ships a three-second test tune, */usr/share/music/demo.dfpwm*. Only images built for this version contain *play*; an existing private root keeps its own files.

*findSound* returns at most 25 matching names. The effect cooldown applies to *playSound*. For an external bus device with tone/PCM support, see the [speaker](../block/speaker.md).
