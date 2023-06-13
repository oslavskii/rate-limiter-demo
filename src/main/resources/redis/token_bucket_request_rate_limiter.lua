local key = KEYS[1]
local refill_tokens_per_second = tonumber(ARGV[1])
local bucket_size = tonumber(ARGV[2])
local current_timestamp_in_seconds = tonumber(ARGV[3])
local tokens_requested = tonumber(ARGV[4])

local last_timestamp_in_seconds = tonumber(redis.call("HGET", key, "refreshed"))
if last_timestamp_in_seconds == nil then
  last_timestamp_in_seconds = 0
end

local tokens_left_last_time = tonumber(redis.call("HGET", key, "tokens"))
if tokens_left_last_time == nil then
  tokens_left_last_time = bucket_size
end

local seconds_passed = math.max(0, current_timestamp_in_seconds - last_timestamp_in_seconds)
local filled_tokens = math.min(bucket_size, tokens_left_last_time + seconds_passed * refill_tokens_per_second)

local allowed = filled_tokens >= tokens_requested
local tokens_left_now = filled_tokens
if allowed then
  tokens_left_now = filled_tokens - tokens_requested
end

redis.call("HSET", key, "tokens", tokens_left_now)
redis.call("HSET", key, "refreshed", current_timestamp_in_seconds)
redis.call("EXPIRE", key, math.ceil(bucket_size / refill_tokens_per_second))

return allowed