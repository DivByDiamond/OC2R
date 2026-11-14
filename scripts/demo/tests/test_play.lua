-- Host test for src/main/scripts/bin/play.lua with a stub of the "devices" module and a scripted
-- terminal. No Minecraft, no tty. Run:  lua5.4 scripts/demo/tests/test_play.lua

local here = arg[0]:match("^(.*)/[^/]*$") or "."
local playPath = here .. "/../../../src/main/scripts/bin/play.lua"

OC2R_PLAYER_AS_MODULE = true
local player = dofile(playPath)

local failures, checks = 0, 0
local function check(cond, message)
    checks = checks + 1
    if not cond then
        failures = failures + 1
        io.stderr:write("FAIL: " .. message .. "\n")
    end
end

-- Fake clock: every terminal read is one tick of 100 ms, like the real "time 1" read.
local function newWorld(opts)
    opts = opts or {}
    local world = { t = 0, queued = 0, queuedAt = 0, writes = {}, attempts = {}, stream = {},
                    refuse = opts.refuse or 0, enters = 0, leaves = 0, ticks = 0, frames = {} }

    local function drain()
        world.queued = math.max(0, world.queued - (world.t - world.queuedAt))
        world.queuedAt = world.t
    end

    local device = {}
    function device:getBufferedMs()
        if opts.failBuffered and world.ticks >= opts.failBuffered then
            error("bus failure")
        end
        drain()
        return world.queued
    end
    function device:writeDfpwm(data, rate)
        drain()
        world.attempts[#world.attempts + 1] = data
        if world.refuse > 0 then
            world.refuse = world.refuse - 1
            return false
        end
        if world.queued > 2000 then
            return false
        end
        world.queued = world.queued + #data * 8000 / rate
        world.writes[#world.writes + 1] = { first = data[1], len = #data, tick = world.ticks }
        for _, b in ipairs(data) do
            world.stream[#world.stream + 1] = b
        end
        return true
    end
    world.device = device

    local term = {}
    function term:enter() world.enters = world.enters + 1 end
    function term:leave() world.leaves = world.leaves + 1 end
    function term:write(text) world.frames[#world.frames + 1] = text end
    function term:readKey()
        world.ticks = world.ticks + 1
        world.t = world.t + 100
        if world.ticks > 5000 then
            error("test runaway: the player never stopped")
        end
        return opts.keys(world.ticks, world)
    end
    world.term = term
    world.env = { device = device, term = term }
    return world
end

local function makeFile(size, fill)
    local parts = {}
    for i = 0, size - 1 do
        parts[#parts + 1] = string.char(fill(i))
    end
    local path = os.tmpname()
    local f = assert(io.open(path, "wb"))
    f:write(table.concat(parts))
    f:close()
    return path
end

local function lastFrame(world)
    return world.frames[#world.frames]
end

-- Marks every 512-byte piece with its index, so the first byte of a chunk tells where it came from.
local function blockFill(i) return (i // 512) % 256 end

-- 1. Chunks go out in order and the whole file arrives; the screen shows name, times and state.
do
    local path = makeFile(48000, function(i) return i % 251 end) -- 8 seconds at 48000 Hz
    local world = newWorld { keys = function(tick, w)
        if w.finishedSeen then return "q" end
        if lastFrame(w) and lastFrame(w):find("Finished", 1, true) then w.finishedSeen = true end
    end }
    local f = io.open(path, "rb")
    local result = player.playFile(world.env, f, "song.dfpwm", 48000)
    f:close()
    check(result == "quit", "q ends playFile")
    local expected = {}
    local fh = io.open(path, "rb")
    local content = fh:read("a")
    fh:close()
    for i = 1, #content do expected[i] = content:byte(i) end
    check(#world.stream == #expected, "all bytes were sent once (" .. #world.stream .. " of " .. #expected .. ")")
    local same = true
    for i = 1, #expected do
        if world.stream[i] ~= expected[i] then same = false break end
    end
    check(same, "bytes were sent in file order")
    local joined = table.concat(world.frames)
    check(joined:find("song.dfpwm", 1, true) ~= nil, "file name is shown")
    check(joined:find("0:08", 1, true) ~= nil, "total time 0:08 is shown")
    check(joined:find("Playing", 1, true) ~= nil, "Playing state is shown")
    check(lastFrame(world):find("Finished", 1, true) ~= nil, "Finished is shown at the end")
    check(lastFrame(world):find("0:08 / 0:08", 1, true) ~= nil, "elapsed reaches the total at the end")
    os.remove(path)
end

-- 2. A false answer: the same data is offered again, nothing is skipped or reordered.
do
    local path = makeFile(10000, function(i) return i % 251 end)
    local world = newWorld { refuse = 4, keys = function(tick, w)
        if #w.stream >= 10000 then return "q" end
    end }
    local f = io.open(path, "rb")
    player.playFile(world.env, f, "x.dfpwm", 48000)
    f:close()
    check(#world.attempts >= 5, "refused attempts were retried")
    local identical = true
    for i = 2, 5 do
        if #world.attempts[i] ~= #world.attempts[1] or world.attempts[i][1] ~= world.attempts[1][1]
            or world.attempts[i][#world.attempts[i]] ~= world.attempts[1][#world.attempts[1]] then
            identical = false
        end
    end
    check(identical, "the refused chunk was offered again unchanged")
    check(#world.stream == 10000, "stream length is exact after refusals")
    local ok = true
    for i = 1, #world.stream do
        if world.stream[i] ~= (i - 1) % 251 then ok = false break end
    end
    check(ok, "stream is in order after refusals")
    os.remove(path)
end

-- 3. Pause stops sending; resume continues.
do
    local path = makeFile(200000, blockFill)
    local counts = {}
    local world = newWorld { keys = function(tick, w)
        counts[tick] = #w.stream
        if tick == 10 then return " " end
        if tick == 60 then return " " end
        if tick == 100 then return "q" end
    end }
    local f = io.open(path, "rb")
    player.playFile(world.env, f, "p.dfpwm", 48000)
    f:close()
    check(counts[11] == counts[59], "nothing is sent while paused (" .. counts[11] .. " vs " .. counts[59] .. ")")
    check(counts[99] > counts[60], "sending resumes after the second space")
    check(table.concat(world.frames):find("Paused", 1, true) ~= nil, "Paused state is shown")
    os.remove(path)
end

-- 4. Seeking and restart.
do
    local path = makeFile(100000, blockFill) -- 195 pieces of 512 bytes, 16.7 s at 48000 Hz
    local before, afterRight, afterLeft, afterRestart
    local world = newWorld { keys = function(tick, w)
        if tick == 20 then w.mark = #w.writes return "right" end
        if tick == 30 then afterRight = w.writes[w.mark + 1] end
        if tick == 40 then w.mark3 = #w.writes return "left" end
        if tick == 60 then afterLeft = w.writes[w.mark3 + 1] end
        if tick == 61 then w.mark4 = #w.writes return "r" end
        if tick == 70 then afterRestart = w.writes[w.mark4 + 1] return "q" end
    end }
    local f = io.open(path, "rb")
    player.playFile(world.env, f, "s.dfpwm", 48000)
    f:close()
    local function offsetOf(write) return write.first * 512 end
    check(afterRight ~= nil, "a chunk was sent after seeking right")
    -- 20 ticks of 100 ms is about 2 s of playback; +5 s lands at about 7 s = 42000 bytes, block aligned.
    local right = afterRight and offsetOf(afterRight) or -1
    check(right % 8192 == 0, "seek target is a multiple of 8192 (" .. right .. ")")
    check(right >= 32768 and right <= 49152, "seek right lands about 5 s ahead (" .. right .. ")")
    local left = afterLeft and offsetOf(afterLeft) or -1
    check(afterLeft ~= nil and left % 8192 == 0, "left seek target is block aligned (" .. left .. ")")
    check(left < right + 20000 and left >= 0, "left seek does not go forward (" .. left .. ")")
    check(afterRestart ~= nil and offsetOf(afterRestart) == 0, "r restarts from byte 0")
    os.remove(path)
end

-- 4b. Seeking left at the very start stays at 0; right past the end does not crash.
do
    local path = makeFile(30000, blockFill)
    local first
    local world = newWorld { keys = function(tick, w)
        if tick == 2 then return "left" end
        if tick == 3 then first = w.writes[#w.writes] end
        if tick >= 4 and tick < 20 then return "right" end
        if tick == 40 then return "q" end
    end }
    local f = io.open(path, "rb")
    local ok, err = pcall(player.playFile, world.env, f, "e.dfpwm", 48000)
    f:close()
    check(ok, "seeking around the edges does not raise (" .. tostring(err) .. ")")
    check(first ~= nil and first.first == 0, "left at the start stays at the start")
    os.remove(path)
end

-- 5. The terminal is restored on quit and on an error.
do
    local path = makeFile(5000, blockFill)
    local world = newWorld { keys = function(tick) if tick == 5 then return "q" end end }
    local ok, err = player.interactive(world.env, path, 48000)
    check(ok == true and err == nil, "interactive returns true on q")
    check(world.enters == 1 and world.leaves == 1, "terminal entered and left once on quit")

    local world2 = newWorld { failBuffered = 3, keys = function() return nil end }
    local ok2, err2 = player.interactive(world2.env, path, 48000)
    check(ok2 == nil and tostring(err2):find("bus failure", 1, true) ~= nil, "an error is reported")
    check(world2.enters == 1 and world2.leaves == 1, "terminal left after an error")

    local world3 = newWorld { keys = function() return "q" end }
    local ok3 = player.interactive(world3.env, path .. ".missing", 48000)
    check(ok3 == nil and world3.leaves == 1, "terminal left when the file is missing")
    os.remove(path)
end

-- 6. The list: Down then Enter plays the second file; q in the player returns to the list; q quits.
do
    local dir = os.tmpname()
    os.remove(dir)
    os.execute("mkdir " .. dir)
    for _, name in ipairs { "b.dfpwm", "a.dfpwm", "notes.txt", "C.DFPWM" } do
        local f = io.open(dir .. "/" .. name, "wb")
        f:write(string.rep("\85", 4096))
        f:close()
    end
    local files = player.listFiles(dir)
    check(#files == 3 and files[1] == "C.DFPWM" and files[2] == "a.dfpwm" and files[3] == "b.dfpwm",
        "listFiles finds only .dfpwm files, sorted")
    local script = { "down", "\r", "q", "q" }
    local n = 0
    local world = newWorld { keys = function(tick, w)
        -- let the player run a few ticks before the first q
        if tick == 1 then return script[1] end
        if tick == 2 then return script[2] end
        if tick == 8 then return script[3] end
        if tick == 9 then return script[4] end
    end }
    local ok = player.interactive(world.env, dir, 48000)
    check(ok == true and world.leaves == 1, "list flow ends cleanly")
    check(world.stream[1] == 85 and #world.stream > 0, "a chosen file was played")
    check(table.concat(world.frames):find("a.dfpwm", 1, true) ~= nil, "list shows the files")
    check(table.concat(world.frames):find("File  a.dfpwm", 1, true) ~= nil, "second entry (a.dfpwm) was chosen")
    os.execute("rm -rf " .. dir)
end

-- 7. Helpers and simple mode.
do
    for _, rate in ipairs { 8000, 11025, 12000, 16000, 24000, 32000, 48000 } do
        check(8192 % player.chunkSize(rate) == 0, "chunk size divides the seek block at " .. rate)
        check(player.chunkSize(rate) * 8000 / rate <= 1100, "chunk is about a second or less at " .. rate)
    end
    check(player.formatTime(75.9) == "1:15" and player.formatTime(0) == "0:00", "formatTime")

    local path = makeFile(10000, function(i) return i % 251 end)
    local world = newWorld { refuse = 3, keys = function() end }
    local slept = 0
    local ok = player.simple(world.device, path, 48000, function(s) slept = slept + s; world.t = world.t + s * 1000 end)
    check(ok == true and #world.stream == 10000, "simple mode sends the whole file")
    check(slept > 0, "simple mode waits on false")
    os.remove(path)
end

if failures > 0 then
    io.stderr:write(string.format("test_play.lua: %d of %d checks failed\n", failures, checks))
    os.exit(1)
end
print(string.format("test_play.lua: %d checks passed", checks))
