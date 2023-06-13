package com.rate.limiter.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new StringRedisSerializer());
    return redisTemplate;
  }

  @Bean
  public RedisScript<Boolean> tokenBucketRequestRateLimiterScript() {
    Resource scriptSource = new ClassPathResource("redis/token_bucket_request_rate_limiter.lua");
    return RedisScript.of(scriptSource, Boolean.class);
  }

  @Bean
  public RedisScript<Boolean> slidingWindowLogRequestRateLimiterScript() {
    Resource scriptSource =
        new ClassPathResource("redis/sliding_window_log_request_rate_limiter.lua");
    return RedisScript.of(scriptSource, Boolean.class);
  }
}
