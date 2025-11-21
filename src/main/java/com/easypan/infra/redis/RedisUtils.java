package com.easypan.infra.redis;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils<V> {

    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    @Resource
    private RedisTemplate<String, V> stringObjectRedisTemplate;

    /**
     * 删除一个或多个 key
     */
    public void delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            stringObjectRedisTemplate.delete(Arrays.asList(keys));
        } catch (Exception e) {
            logger.error("删除 redis key 失败, keys={}", Arrays.toString(keys), e);
            // 是否抛异常看你项目风格，这里暂时只是打日志
        }
    }

    /**
     * 根据 key 获取值
     */
    public V get(String key) {
        if (key == null) {
            return null;
        }
        try {
            return stringObjectRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("从 redis 获取 key={} 失败", key, e);
            return null;
        }
    }

    /**
     * 普通缓存放入
     *
     * @param key   键（不能为空）
     * @param value 值
     * @return true 成功，false 失败
     */
    public boolean set(String key, V value) {
        if (key == null) {
            logger.warn("尝试使用 null 作为 redis key，已忽略");
            return false;
        }
        try {
            stringObjectRedisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("设置 redis key={} value={} 失败", key, value, e);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置过期时间
     *
     * @param key      键（不能为空）
     * @param value    值
     * @param time     过期时间
     * @param timeUnit 过期时间单位
     * @return true 成功，false 失败
     */
    public boolean setex(String key, V value, long time, TimeUnit timeUnit) {
        if (key == null) {
            logger.warn("尝试使用 null 作为 redis key，已忽略");
            return false;
        }
        try {
            if (time > 0 && timeUnit != null) {
                stringObjectRedisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                // time <= 0 或 timeUnit == null 时，当作永久不过期
                stringObjectRedisTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("设置 redis key={} value={} 失败", key, value, e);
            return false;
        }
    }

    /**
     * 数值自增（必须确保该 key 存的是数值类型，底层使用 Redis INCRBY）
     *
     * @param key   redis key
     * @param delta 增加的值（可以为负，等价于 DECRBY）
     * @return 自增后的最新值，失败时返回 null
     */
    public Long incrBy(String key, long delta) {
        if (key == null) {
            logger.warn("尝试对 null key 做自增操作，已忽略");
            return null;
        }
        try {
            // 这里底层会直接发 Redis INCRBY 命令
            return stringObjectRedisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            logger.error("对 redis key={} 执行 INCRBY {} 失败", key, delta, e);
            return null;
        }
    }

    /**
     * 数值自增并重置过期时间
     *
     * @param key   redis key
     * @param delta 增加的值
     * @param time  过期时间
     * @param unit  时间单位
     * @return 自增后的最新值，失败时返回 null
     */
    public Long incrByWithExpire(String key, long delta, long time, TimeUnit unit) {
        Long val = incrBy(key, delta);
        if (val == null) {
            return null;
        }
        if (time > 0 && unit != null) {
            try {
                stringObjectRedisTemplate.expire(key, time, unit);
            } catch (Exception e) {
                logger.error("设置 redis key={} 过期时间失败", key, e);
            }
        }
        return val;
    }
}

