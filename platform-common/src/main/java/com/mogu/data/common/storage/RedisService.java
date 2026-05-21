package com.mogu.data.common.storage;

import com.mogu.data.common.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis操作服务
 * 用于缓存和在线特征存储
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private static final Logger log = Logger.getLogger(RedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存并设置过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断key是否存在
     */
    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * Hash操作 - 设置
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Hash操作 - 获取
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * Hash操作 - 获取所有
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash操作 - 删除
     */
    public Long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * Hash操作 - 判断字段是否存在
     */
    public Boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * List操作 - 左侧推入
     */
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * List操作 - 右侧推入
     */
    public Long rPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * List操作 - 左侧弹出
     */
    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * List操作 - 右侧弹出
     */
    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * List操作 - 获取范围
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * Set操作 - 添加
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * Set操作 - 获取所有
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Set操作 - 删除
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * Set操作 - 判断是否存在
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * ZSet操作 - 添加
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * ZSet操作 - 获取范围
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * ZSet操作 - 按分数获取范围
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * ZSet操作 - 删除
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * Scan操作 - 扫描匹配的key
     *
     * @param pattern key模式（如 "user:*"）
     * @param count 每次扫描的数量
     * @return 匹配的key集合
     */
    public Set<String> scan(String pattern, int count) {
        try {
            Set<String> keys = new java.util.HashSet<>();
            org.springframework.data.redis.core.ScanOptions options =
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(count)
                    .build();

            org.springframework.data.redis.core.Cursor<String> cursor =
                redisTemplate.scan(options);

            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }

            try {
                cursor.close();
            } catch (Exception e) {
                log.error("关闭Redis cursor失败", e);
            }

            return keys;

        } catch (Exception e) {
            log.error("Redis scan失败: " + e.getMessage(), e);
            throw new RuntimeException("Redis scan失败", e);
        }
    }

    /**
     * Scan操作 - 扫描匹配的key（默认count=1000）
     */
    public Set<String> scan(String pattern) {
        return scan(pattern, 1000);
    }

    /**
     * 批量获取值
     *
     * @param keys key集合
     * @return key -> value 的映射
     */
    public Map<String, Object> multiGet(Set<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return new java.util.HashMap<>();
            }

            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            Map<String, Object> result = new java.util.HashMap<>();

            int i = 0;
            for (String key : keys) {
                if (values != null && i < values.size() && values.get(i) != null) {
                    result.put(key, values.get(i));
                }
                i++;
            }

            return result;

        } catch (Exception e) {
            log.error("批量获取Redis值失败", e);
            throw new RuntimeException("批量获取失败", e);
        }
    }

    /**
     * 批量设置值
     *
     * @param map key-value映射
     */
    public void multiSet(Map<String, Object> map) {
        try {
            if (map == null || map.isEmpty()) {
                return;
            }

            redisTemplate.opsForValue().multiSet(map);

        } catch (Exception e) {
            log.error("批量设置Redis值失败", e);
            throw new RuntimeException("批量设置失败", e);
        }
    }

    /**
     * 获取匹配模式的所有key-value对
     *
     * @param pattern key模式
     * @return key-value映射
     */
    public Map<String, Object> getPattern(String pattern) {
        Set<String> keys = scan(pattern);
        return multiGet(keys);
    }

    /**
     * 统计匹配模式的key数量
     *
     * @param pattern key模式
     * @return key数量
     */
    public long countKeys(String pattern) {
        try {
            Set<String> keys = scan(pattern, 100);
            return keys.size();

        } catch (Exception e) {
            log.error("统计Redis key数量失败", e);
            return 0;
        }
    }

    /**
     * 删除匹配模式的所有key
     *
     * @param pattern key模式
     * @return 删除的key数量
     */
    public Long deletePattern(String pattern) {
        try {
            Set<String> keys = scan(pattern);
            if (keys.isEmpty()) {
                return 0L;
            }

            return redisTemplate.delete(keys);

        } catch (Exception e) {
            log.error("批量删除Redis key失败", e);
            throw new RuntimeException("批量删除失败", e);
        }
    }
}