package com.ssafy.zipdaum.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    // 문자열 데이터 가져오기
    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // TTL과 함께 데이터 저장
    public void setDataWithTTL(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    // 데이터 존재 여부
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 데이터 삭제
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
