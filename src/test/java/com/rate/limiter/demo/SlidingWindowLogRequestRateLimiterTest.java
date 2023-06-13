package com.rate.limiter.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SpringBootTest
class SlidingWindowLogRequestRateLimiterTest {

  private static final String TEST_USER_ID = "test_user_id";
  private static final int WINDOW_SIZE_IN_MICROSECONDS = 1;
  private static final int WINDOW_LIMIT = 1;
  private static final int MICROSECONDS_IN_SECOND = 1_000_000;

  @Autowired private RedisTemplate<String, String> redisTemplate;
  @Autowired private RedisScript<Boolean> slidingWindowLogRequestRateLimiterScript;
  private SlidingWindowLogRequestRateLimiter tested;

  @BeforeEach
  void setUp() {
    tested =
        new SlidingWindowLogRequestRateLimiter(
            redisTemplate,
            slidingWindowLogRequestRateLimiterScript,
            WINDOW_SIZE_IN_MICROSECONDS,
            WINDOW_LIMIT);
  }

  @AfterEach
  void tearDown() {
    tested.reset(TEST_USER_ID);
  }

  @Test
  void checkRequestRateLimiter_windowIsDrained_requestIsNotAllowed() {
    // 00:00:01 : drain the window limit
    for (int i = 0; i < WINDOW_LIMIT; i++)
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1 * MICROSECONDS_IN_SECOND));

    // 00:00:01 : same window, request not allowed
    assertFalse(tested.checkRequestRateLimiter(TEST_USER_ID, 1 * MICROSECONDS_IN_SECOND));
  }

  @Test
  void checkRequestRateLimiter_windowIsDrained_requestIsAllowedInNewWindow() {
    // 00:00:01 : drain the window limit
    for (int i = 0; i < WINDOW_LIMIT; i++)
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1 * MICROSECONDS_IN_SECOND));

    // 00:00:02 : new window, so request allowed
    assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 2 * MICROSECONDS_IN_SECOND));
  }

  @Test
  void checkRequestRateLimiter_burstAtTheEdgeOfTheWindow_noMoreThanWindowLimitRequestsAllowed() {
    var allowed = 0;
    var requestsAtTheEndOfFirstWindow = 1 + new Random().nextInt(0, WINDOW_LIMIT);
    for (int i = 0; i < requestsAtTheEndOfFirstWindow; i++)
      if (tested.checkRequestRateLimiter(TEST_USER_ID, 1_999_999)) allowed++;

    for (int i = 0; i < WINDOW_LIMIT; i++)
      if (tested.checkRequestRateLimiter(TEST_USER_ID, 2_000_001)) allowed++;

    assertEquals(WINDOW_LIMIT, allowed);
  }
}
