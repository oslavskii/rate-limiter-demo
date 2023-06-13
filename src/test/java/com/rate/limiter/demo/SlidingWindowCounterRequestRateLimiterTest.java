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
class SlidingWindowCounterRequestRateLimiterTest {

  private static final String TEST_USER_ID = "test_user_id";
  private static final int WINDOW_SIZE_IN_SECONDS = 1;
  private static final int WINDOW_LIMIT = 7;

  @Autowired private RedisTemplate<String, String> redisTemplate;
  @Autowired private RedisScript<Boolean> slidingWindowCounterRequestRateLimiterScript;
  private SlidingWindowCounterRequestRateLimiter tested;

  @BeforeEach
  void setUp() {
    tested =
        new SlidingWindowCounterRequestRateLimiter(
            redisTemplate,
            slidingWindowCounterRequestRateLimiterScript,
            WINDOW_SIZE_IN_SECONDS,
            WINDOW_LIMIT);
  }

  @AfterEach
  void tearDown() {
    tested.reset(TEST_USER_ID + ".0");
    tested.reset(TEST_USER_ID + ".1");
    tested.reset(TEST_USER_ID + ".2");
  }

  @Test
  void checkRequestRateLimiter_windowIsDrained_requestIsNotAllowed() {
    // 00:00:01 : drain the window
    for (int i = 0; i < WINDOW_LIMIT; i++) {
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1000));
    }

    // 00:00:01 : request is not allowed
    assertFalse(tested.checkRequestRateLimiter(TEST_USER_ID, 1000));
  }

  @Test
  void checkRequestRateLimiter_windowIsDrained_requestIsNotAllowedAtTheStartOfNewWindow() {
    // 00:00:01 : drain the window limit
    for (int i = 0; i < WINDOW_LIMIT; i++) {
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1000));
    }

    // memory trade-off: assume requests were evenly distributed in previous window
    // weighted request count: 1 + 7 * 90% = 7.3 requests, more than limit => not allow
    assertFalse(tested.checkRequestRateLimiter(TEST_USER_ID, 2100));
  }

  @Test
  void checkRequestRateLimiter_windowIsDrained_requestIsAllowedInCompletelyNewWindow() {
    // 00:00:01 : drain the window limit
    for (int i = 0; i < WINDOW_LIMIT; i++) {
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1000));
    }

    // 00:00:03 : new window, so request allowed
    assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 3000));
  }

  @Test
  void checkRequestRateLimiter_weightedCounter() {
    // 5 requests in previous window
    for (int i = 0; i < 5; i++) {
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 1000));
    }

    // 2 requests in current window
    for (int i = 0; i < 2; i++) {
      assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 2000));
    }

    // 30% of window passed
    // weighted request count: 3 + 5 * 70% = 6.5 requests, less than limit => allow
    assertTrue(tested.checkRequestRateLimiter(TEST_USER_ID, 2300));

    // 30% of window has passed
    // weighted request count: 4 + 5 * 70% = 7.5 requests, more than limit => not allow
    assertFalse(tested.checkRequestRateLimiter(TEST_USER_ID, 2300));
  }

  @Test
  void checkRequestRateLimiter_burstAtTheEdgeOfTheWindow_noMoreThanWindowLimitRequestsAllowed() {
    var allowed = 0;
    var requestsAtTheEndOfFirstWindow = 1 + new Random().nextInt(0, WINDOW_LIMIT);
    for (int i = 0; i < requestsAtTheEndOfFirstWindow; i++)
      if (tested.checkRequestRateLimiter(TEST_USER_ID, 1999)) allowed++;

    for (int i = 0; i < WINDOW_LIMIT; i++)
      if (tested.checkRequestRateLimiter(TEST_USER_ID, 2001)) allowed++;

    assertEquals(WINDOW_LIMIT, allowed);
  }
}
