#!/usr/bin/lua

-- Plays a DFPWM file on a speaker or a sound card.
--   play.lua [-r RATE] FILE.dfpwm
-- RATE is the sample rate the file was recorded at (8000-48000, default 48000).

local devices = require("devices")

local rate = 48000
local path
local i = 1
while i <= #arg do
    if arg[i] == "-r" then
        rate = tonumber(arg[i + 1])
        i = i + 2
    else
        path = arg[i]
        i = i + 1
    end
end

if not path or not rate then
    io.stderr:write("usage: play.lua [-r RATE] FILE.dfpwm\n")
    os.exit(1)
end

local device = devices:find("speaker") or devices:find("sound")
if not device then
    io.stderr:write("A speaker or a sound card is required.\n")
    os.exit(1)
end

local file, err = io.open(path, "rb")
if not file then
    io.stderr:write(err .. "\n")
    os.exit(1)
end

local CHUNK = 4096 -- 4096 bytes are about 0.7 seconds of audio at 48000 Hz

local function toBytes(chunk)
    local bytes = {}
    for from = 1, #chunk, 256 do
        local part = {chunk:byte(from, math.min(from + 255, #chunk))}
        for _, value in ipairs(part) do
            bytes[#bytes + 1] = value
        end
    end
    return bytes
end

while true do
    local chunk = file:read(CHUNK)
    if not chunk then
        break
    end
    local bytes = toBytes(chunk)
    -- writeDfpwm returns false while about two seconds are already queued; wait and offer the same data again.
    while not device:writeDfpwm(bytes, rate) do
        os.execute("sleep 0.2")
    end
end
file:close()

-- Let the last queued audio finish before returning to the prompt.
local remaining = device:getBufferedMs()
if remaining > 0 then
    os.execute(string.format("sleep %.2f", remaining / 1000))
end
