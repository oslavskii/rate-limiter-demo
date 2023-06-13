local key = KEYS[1]
local window_size_in_seconds = tonumber(ARGV[1])
local window_limit = tonumber(ARGV[2])
local current_time_in_milliseconds = tonumber(ARGV[3])

local current_window_start = math.floor(current_time_in_milliseconds / 1000 / window_size_in_seconds)
local current_window_key = key .. "." .. current_window_start

local previous_window_start = current_window_start - 1
local previous_window_key = key .. "." .. previous_window_start

local current_window_count = redis.call("INCR", current_window_key)
redis.call("EXPIRE", current_window_key, window_size_in_seconds * 2)

local previous_window_count = tonumber(redis.call("GET", previous_window_key))
if previous_window_count == nil then
  previous_window_count = 0
end

local current_previous_windows_overlap = 1 - (current_time_in_milliseconds - current_window_start * 1000) / (window_size_in_seconds * 1000)
local weighted_request_count = current_window_count + previous_window_count * current_previous_windows_overlap
local allowed = weighted_request_count <= window_limit

return allowed