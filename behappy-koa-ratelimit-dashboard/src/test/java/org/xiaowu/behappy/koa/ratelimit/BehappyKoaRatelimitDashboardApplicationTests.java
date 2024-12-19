package org.xiaowu.behappy.koa.ratelimit;

import cn.hutool.core.util.NumberUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.xiaowu.behappy.koa.ratelimit.config.MultiRedisConnectionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootTest
class BehappyKoaRatelimitDashboardApplicationTests {
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    MultiRedisConnectionFactory multiRedisConnectionFactory;

    public Object parse(String value) {
        if (NumberUtil.isNumber((value))) {
            return NumberUtil.parseInt(value);
        }else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.valueOf(value);
        }else {
            // 字符串
            return value;
        }
    }

    public List<Map<Object, Object>> findHashKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern + "*");
        List<Map<Object, Object>> result = new ArrayList<>();
        for (String key : keys) {
            Map<Object, Object> hash = redisTemplate.opsForHash().entries(key);
            result.add(hash);
        }
        return result;
    }

    @Test
    void testScanKeyField(){
        // 使用示例
        multiRedisConnectionFactory.setCurrentRedis("default",11);
        String prefix = "BH-RATE-CONFIG";
        List<Map<Object, Object>> maps = findHashKeys(prefix);
        for (Map<Object, Object> map : maps) {
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                System.out.printf("%s 类型是：%s%n",entry.getValue(), parse(((String) entry.getValue())).getClass());
            }
        }
        System.out.println(maps);
    }

}
