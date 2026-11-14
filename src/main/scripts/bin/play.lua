#!/usr/bin/lua

-- DFPWM music player for a speaker or a sound card (terminal UI).
--
--   play.lua [-r RATE] [FILE.dfpwm | DIRECTORY]
--   play.lua --simple [-r RATE] FILE.dfpwm
--
-- Run it with --help for the keys and the notes on seeking.

local USAGE = [[
usage: play.lua [-r RATE] [FILE.dfpwm | DIRECTORY]
       play.lua --simple [-r RATE] FILE.dfpwm

Plays DFPWM audio on the first speaker or sound card of the machine.

  -r RATE     sample rate the file was recorded at, 8000-48000 (default 48000)
  --simple    no interface: play one file and exit; works without a terminal
  -h, --help  this text

Without a file the player lists the .dfpwm files of the directory (the current
one by default). In the list: Up/Down move, Enter plays, q quits.

Keys while playing:
  Space       pause and resume
  Left/Right  seek back/forward by about 5 seconds
  r           restart from the beginning
  q           stop (back to the list, or quit)

Notes on seeking: DFPWM is a delta format, so a position can only be reached by
starting from a block boundary (a multiple of 8192 bytes). The decoder on the
other side keeps its state between chunks and only resets after about a second
of silence, so after a seek or a restart the sound may begin with a click or a
short burst of noise until the decoder settles. Audio that was already queued
(up to about a second) still plays out before the new position is heard, and
pause works the same way: it takes effect when the queue runs dry.
]]

local M = {}

local MIN_RATE, MAX_RATE, DEFAULT_RATE = 8000, 48000, 48000
local BLOCK = 8192 -- seek granularity in bytes
local SEEK_SECONDS = 5
local TARGET_BUFFER_MS = 700 -- keep about this much queued; the device refuses at about 2000
local BAR_WIDTH = 50

-- Bytes per write: the bus drops a guest message over 4096 bytes and a byte costs up to four characters of JSON,
-- so 512 bytes (about 0.085 seconds at 48000 Hz) is the largest power of two that always fits; it divides BLOCK.
function M.chunkSize(_)
    return 512
end

function M.formatTime(seconds)
    seconds = math.max(0, math.floor(seconds))
    return string.format("%d:%02d", seconds // 60, seconds % 60)
end

local function toBytes(chunk)
    local bytes = {}
    for from = 1, #chunk, 256 do
        local part = { chunk:byte(from, math.min(from + 255, #chunk)) }
        for _, value in ipairs(part) do
            bytes[#bytes + 1] = value
        end
    end
    return bytes
end

local function shellQuote(text)
    return "'" .. text:gsub("'", "'\\''") .. "'"
end

---------------------------------------------------------------------------
-- Terminal: raw input without Enter and reads that give up after 0.1 s.
-- Only stty and the plain Lua io library are used (the image has no luaposix
-- for this): "min 0 time 1" makes read(2) return after a tenth of a second
-- without data, and the file is unbuffered so every read goes to the tty.
---------------------------------------------------------------------------

local ESC_KEYS = { A = "up", B = "down", C = "right", D = "left" }

function M.newTerminal(ttyPath)
    ttyPath = ttyPath or "/dev/tty"
    local term = { path = ttyPath }

    function term:enter()
        local handle = io.popen("stty -F " .. shellQuote(ttyPath) .. " -g 2>/dev/null")
        local saved = handle and handle:read("l")
        if handle then
            handle:close()
        end
        self.saved = (saved and saved ~= "") and saved or nil
        local input, err = io.open(ttyPath, "rb")
        if not input then
            error("cannot open " .. ttyPath .. ": " .. tostring(err), 0)
        end
        input:setvbuf("no")
        self.input = input
        os.execute("stty -F " .. shellQuote(ttyPath) .. " raw -echo min 0 time 1")
        io.stdout:write("\27[?1049h\27[?25l")
        io.stdout:flush()
        self.active = true
    end

    function term:leave()
        if not self.active then
            return
        end
        self.active = false
        io.stdout:write("\27[?25h\27[?1049l")
        io.stdout:flush()
        local settings = self.saved and shellQuote(self.saved) or "sane"
        os.execute("stty -F " .. shellQuote(ttyPath) .. " " .. settings)
        if self.input then
            self.input:close()
            self.input = nil
        end
    end

    -- Returns a key name or character, or nil when nothing was typed for 0.1 s.
    function term:readKey()
        local c = self.input:read(1)
        if c == nil then
            return nil
        end
        if c ~= "\27" then
            return c
        end
        local c2 = self.input:read(1)
        if c2 ~= "[" and c2 ~= "O" then
            return "esc"
        end
        local c3 = self.input:read(1)
        while c3 and c3:match("[%d;]") do
            c3 = self.input:read(1)
        end
        return ESC_KEYS[c3] or "unknown"
    end

    function term:write(text)
        io.stdout:write(text)
        io.stdout:flush()
    end

    return term
end

---------------------------------------------------------------------------
-- Playback screen
---------------------------------------------------------------------------

-- Plays one open file until it ends, the user stops it, or an error occurs.
-- env: device, term (readKey, write), name. Returns "quit" or "done".
function M.playFile(env, file, name, rate)
    local device, term = env.device, env.term
    local size = file:seek("end")
    file:seek("set", 0)
    local bytesPerSecond = rate / 8
    local total = size / bytesPerSecond
    local chunkSize = M.chunkSize(rate)

    local pos = 0 -- file offset of the next byte to send
    local seekFloor = 0 -- the position shown cannot be lower than the last seek target
    local pending -- bytes read from the file but not accepted by the device yet
    local pendingLen = 0
    local paused = false
    local ended = false
    local lastFrame
    local buffered = 0

    local function position()
        local played = math.max(seekFloor, pos - buffered * bytesPerSecond / 1000)
        return math.min(played, size) / bytesPerSecond
    end

    local function seekTo(offset)
        offset = math.max(0, math.min(offset, size))
        offset = offset - offset % BLOCK
        file:seek("set", offset)
        pos = offset
        seekFloor = offset
        pending = nil
        ended = false
    end

    local function seekBy(seconds)
        local steps = math.max(1, math.floor(SEEK_SECONDS * bytesPerSecond / BLOCK + 0.5))
        local current = position() * bytesPerSecond
        local target = current + (seconds < 0 and -steps or steps) * BLOCK
        -- Seeking forward past the last block leaves the last block playing.
        if target >= size then
            target = math.max(0, size - 1)
        end
        seekTo(target)
    end

    local function draw(state)
        local shown = position()
        local fraction = total > 0 and math.min(1, shown / total) or 0
        local filled = math.floor(fraction * BAR_WIDTH + 0.5)
        local lines = {
            " OC2R music player",
            "",
            " File  " .. name,
            " Rate  " .. rate .. " Hz",
            "",
            string.format(" [%s%s]  %s / %s", string.rep("#", filled), string.rep("-", BAR_WIDTH - filled),
                M.formatTime(shown), M.formatTime(total)),
            "",
            " " .. state,
            "",
            " Space pause   Left/Right seek " .. SEEK_SECONDS .. "s   r restart   q stop",
        }
        local frame = table.concat(lines, "\27[K\n") .. "\27[K"
        if frame ~= lastFrame then
            lastFrame = frame
            term:write("\27[H" .. frame .. "\27[J")
        end
    end

    term:write("\27[2J")
    while true do
        buffered = device:getBufferedMs() or 0

        -- Keep the device queue topped up. A false answer means the queue is full:
        -- keep the same bytes and offer them again on the next tick.
        if not paused and not ended and buffered < TARGET_BUFFER_MS then
            if not pending then
                local chunk = file:read(chunkSize)
                if chunk then
                    pending = toBytes(chunk)
                    pendingLen = #chunk
                else
                    ended = true
                end
            end
            if pending and device:writeDfpwm(pending, rate) then
                pos = pos + pendingLen
                pending = nil
            end
        end

        local state
        if ended and buffered == 0 then
            state = "Finished"
        elseif paused then
            state = "Paused"
        else
            state = "Playing"
        end
        draw(state)

        -- This read is also the pause between ticks: it returns after about 0.1 s.
        local key = term:readKey()
        if key == "q" or key == "\3" then
            return "quit"
        elseif key == " " then
            paused = not paused
        elseif key == "r" then
            seekTo(0)
            paused = false
        elseif key == "left" then
            seekBy(-1)
        elseif key == "right" then
            seekBy(1)
        end
    end
end

---------------------------------------------------------------------------
-- File list
---------------------------------------------------------------------------

function M.listFiles(dir)
    local files = {}
    local handle = io.popen("ls -1 " .. shellQuote(dir) .. " 2>/dev/null")
    if handle then
        for line in handle:lines() do
            if line:lower():match("%.dfpwm$") then
                files[#files + 1] = line
            end
        end
        handle:close()
    end
    table.sort(files)
    return files
end

local function joinPath(dir, name)
    if dir:sub(-1) == "/" then
        return dir .. name
    end
    return dir .. "/" .. name
end

-- Shows the list; returns the chosen file name, or nil when the user quits.
function M.chooseFile(env, dir, files)
    local term = env.term
    local selected, top = 1, 1
    local rows = 12
    local lastFrame
    term:write("\27[2J")
    while true do
        if selected < top then
            top = selected
        elseif selected >= top + rows then
            top = selected - rows + 1
        end
        local lines = { " OC2R music player", " " .. dir, "" }
        if #files == 0 then
            lines[#lines + 1] = " No .dfpwm files here."
        end
        for i = top, math.min(#files, top + rows - 1) do
            lines[#lines + 1] = (i == selected and " > " or "   ") .. files[i]
        end
        lines[#lines + 1] = ""
        lines[#lines + 1] = " Up/Down choose   Enter play   q quit"
        local frame = table.concat(lines, "\27[K\n") .. "\27[K"
        if frame ~= lastFrame then
            lastFrame = frame
            term:write("\27[H" .. frame .. "\27[J")
        end

        local key = term:readKey()
        if key == "q" or key == "\3" then
            return nil
        elseif (key == "down" or key == "j") and selected < #files then
            selected = selected + 1
        elseif (key == "up" or key == "k") and selected > 1 then
            selected = selected - 1
        elseif (key == "\r" or key == "\n") and #files > 0 then
            return files[selected]
        end
    end
end

---------------------------------------------------------------------------
-- Entry points
---------------------------------------------------------------------------

local function isDirectory(path)
    local f = io.open(path, "rb")
    if not f then
        return false
    end
    local ok, err = f:read(0)
    f:close()
    return ok == nil and err ~= nil
end

-- Interface-less mode for sessions without a terminal.
function M.simple(device, path, rate, sleep)
    sleep = sleep or function(seconds) os.execute(string.format("sleep %.2f", seconds)) end
    local file, err = io.open(path, "rb")
    if not file then
        return nil, err
    end
    local chunkSize = 512 -- see M.chunkSize: the bus drops messages over 4096 bytes
    while true do
        local chunk = file:read(chunkSize)
        if not chunk then
            break
        end
        local bytes = toBytes(chunk)
        -- writeDfpwm returns false while about two seconds are already queued; wait and offer the same data again.
        while not device:writeDfpwm(bytes, rate) do
            sleep(0.2)
        end
    end
    file:close()
    -- Let the last queued audio finish before returning to the prompt.
    local remaining = device:getBufferedMs()
    if remaining > 0 then
        sleep(remaining / 1000)
    end
    return true
end

-- Runs the interface; the terminal is restored on every way out, errors included.
function M.interactive(env, target, rate)
    local term = env.term
    local ok, result = pcall(function()
        term:enter()
        local dir, single = target, nil
        if not isDirectory(target) then
            single = target
        end
        if single then
            local file, err = io.open(single, "rb")
            if not file then
                error(err, 0)
            end
            local name = single:match("([^/]*)$")
            M.playFile(env, file, name, rate)
            file:close()
            return
        end
        while true do
            local files = M.listFiles(dir)
            local name = M.chooseFile(env, dir, files)
            if not name then
                return
            end
            local file, err = io.open(joinPath(dir, name), "rb")
            if not file then
                error(err, 0)
            end
            local ok2, perr = pcall(M.playFile, env, file, name, rate)
            file:close()
            if not ok2 then
                error(perr, 0)
            end
        end
    end)
    term:leave()
    if not ok then
        return nil, tostring(result)
    end
    return true
end

local function fail(message)
    io.stderr:write(message .. "\n")
    return 1
end

function M.main(args)
    local rate = DEFAULT_RATE
    local simple = false
    local target
    local i = 1
    while i <= #args do
        local a = args[i]
        if a == "-r" then
            rate = tonumber(args[i + 1])
            i = i + 2
        elseif a == "--simple" then
            simple = true
            i = i + 1
        elseif a == "-h" or a == "--help" then
            io.write(USAGE)
            return 0
        elseif a:sub(1, 1) == "-" and #a > 1 then
            return fail("unknown option " .. a .. "\n" .. USAGE)
        else
            target = a
            i = i + 1
        end
    end

    if not rate or rate < MIN_RATE or rate > MAX_RATE then
        return fail("The rate must be a number from " .. MIN_RATE .. " to " .. MAX_RATE .. ".")
    end
    if simple and not target then
        return fail(USAGE)
    end

    local devices = require("devices")
    local device = devices:find("speaker") or devices:find("sound")
    if not device then
        return fail("A speaker or a sound card is required.")
    end

    if simple then
        local ok, err = M.simple(device, target, rate)
        if not ok then
            return fail(err)
        end
        return 0
    end

    local ok, err = M.interactive({ device = device, term = M.newTerminal() }, target or ".", rate)
    if not ok then
        return fail(err)
    end
    return 0
end

if rawget(_G, "OC2R_PLAYER_AS_MODULE") then
    return M
end
os.exit(M.main(arg))
