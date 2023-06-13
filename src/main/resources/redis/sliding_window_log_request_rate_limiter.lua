local key = KEYS[1]
local window_size_in_seconds = tonumber(ARGV[1])
local window_limit = tonumber(ARGV[2])
local current_timestamp_in_microseconds = tonumber(ARGV[3])

redis.call("ZREMRANGEBYSCORE", key, 0, current_timestamp_in_microseconds - window_size_in_seconds * 1000000)

local current_set = redis.call("ZRANGE", key, 0, -1)
local current_set_size = 0
for k, v in pairs(current_set) do
  current_set_size = current_set_size + 1
end

local allowed = current_set_size < window_limit

redis.call("ZADD", key, current_timestamp_in_microseconds, current_timestamp_in_microseconds)
redis.call("EXPIRE", key, math.ceil(window_size_in_seconds))

return allowed