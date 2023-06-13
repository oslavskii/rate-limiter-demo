package com.rate.limiter.demo;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

public class TokenBucketRequestRateLimiter {
  private final RedisTemplate<String, String> redisTemplate;
  private final RedisScript<Boolean> requestRateLimiterScript;
  private final int refillTokensPerSecond;
  private final int bucketSize;

  public TokenBucketRequestRateLimiter(
      RedisTemplate<String, String> redisTemplate,
      RedisScript<Boolean> requestRateLimiterScript,
      int refillTokensPerSecond,
      int bucketSize) {
    this.redisTemplate = redisTemplate;
    this.requestRateLimiterScript = requestRateLimiterScript;
    this.refillTokensPerSecond = refillTokensPerSecond;
    this.bucketSize = bucketSize;
  }

  private static String key(String userId) {
    return "request_rate_limiter." + userId;
  }

  public Boolean checkRequestRateLimiter(String userId, int currentTimestampInSeconds) {
    var keys = List.of(key(userId));
    var args =
        Stream.of(refillTokensPerSecond, bucketSize, currentTimestampInSeconds, 1)
            .map(String::valueOf)
            .toArray();
    return redisTemplate.execute(requestRateLimiterScript, keys, args);
  }

  public void reset(String userId) {
    redisTemplate.delete(key(userId));
  }
}
