package com.rate.limiter.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SpringBootTest
class TokenBucketRequestRateLimiterTests {

  private static final String TEST_USER_ID = "test_user_id";
  private static final int REFILL_TOKENS_PER_SECOND = 1;
  private static final int BUCKET_SIZE = 2;

  @Autowired private RedisTemplate<String, String> redisTemplate;
  @Autowired private RedisScript<Boolean> tokenBucketRequestRateLimiterScript;
  private TokenBucketRequestRateLimiter tested;

  @BeforeEach
  void setUp() {
    tested =
        new TokenBucketRequestRateLimiter(
            redisTemplate,
            tokenBucketRequestRateLimiterScript,
            REFILL_TOKENS_PER_SECOND,
            BUCKET_SIZE);
  }

  @AfterEach
  void tearDown() {
    tested.reset(TEST_USER_ID);
  }

  @Test
  void checkRequestRateLimiter_bucketIsDrained_requestIsNotAllowed() {
    // 00:00:01 : drain the bucket
    for (int i = 0; i < BUCKET_SIZE; i++)
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1));

    // 00:00:01 : no tokens left, request not allowed
    assertFalse(tested.checkRequestRateLimiter(TEST_USER_ID, 1));
  }

  @Test
  void checkRequestRateLimiter_bucketIsDrained_requestIsAllowedAfterRefill() {
    // 00:00:01 : drain the bucket
    for (int i = 0; i < BUCKET_SIZE; i++)
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1));

    // 00:00:02 : one token refilled, so one request allowed
    assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 2));
  }

  @Test
  void checkRequestRateLimiter_bucketIsDrainedAfterRefill_requestIsNotAllowed() {
    // 00:00:01 : drain the bucket
    for (int i = 0; i < BUCKET_SIZE; i++)
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1));

    // 00:00:02 : one token refilled, so one request allowed
    assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 2));

    // 00:00:02 : no tokens left, request not allowed
    assertFalse(tested.checkRequestRateLimiter(TEST_USER_ID, 2));
  }
}
