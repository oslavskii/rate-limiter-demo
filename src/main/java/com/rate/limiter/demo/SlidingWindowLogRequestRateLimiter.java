package com.rate.limiter.demo;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

public class SlidingWindowLogRequestRateLimiter {
  private final RedisTemplate<String, String> redisTemplate;
  private final RedisScript<Boolean> requestRateLimiterScript;
  private final int windowSizeInSeconds;
  private final int windowLimit;

  public SlidingWindowLogRequestRateLimiter(
      RedisTemplate<String, String> redisTemplate,
      RedisScript<Boolean> requestRateLimiterScript,
      int windowSizeInSeconds,
      int windowLimit) {
    this.redisTemplate = redisTemplate;
    this.requestRateLimiterScript = requestRateLimiterScript;
    this.windowSizeInSeconds = windowSizeInSeconds;
    this.windowLimit = windowLimit;
  }

  private static String key(String userId) {
    return "request_rate_limiter." + userId;
  }

  public Boolean checkRequestRateLimiter(String userId, int currentTimeInMicroseconds) {
    var keys = List.of(key(userId));
    var args =
        Stream.of(windowSizeInSeconds, windowLimit, currentTimeInMicroseconds)
            .map(String::valueOf)
            .toArray();
    return redisTemplate.execute(requestRateLimiterScript, keys, args);
  }

  public void reset(String userId) {
    redisTemplate.delete(key(userId));
  }
}
