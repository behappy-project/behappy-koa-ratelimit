package org.xiaowu.behappy.koa.ratelimit.service;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xiaowu.behappy.koa.ratelimit.config.MultiRedisConnectionFactory;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitConfigRQ;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitConfigRS;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitSwitchRQ;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitSwitchRS;

import java.util.*;

/**
 * @author xiaowu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final MultiRedisConnectionFactory multiRedisConnectionFactory;

    private final RedisTemplate<String, Object> redisTemplate;

    private final static String RATE_CONFIG_PREFIX = "BH-RATE-CONFIG:";

    private final static String RATE_SWITCH_PREFIX = "BH-RATE-SWITCH:";

    private final static String RATE_LIMIT_HOT_PREFIX = "BH-RATE-HOT:";

    public boolean saveOrUpdateRateLimitSwitch(ModuleRateLimitSwitchRQ moduleRateLimitRQ) {
        multiRedisConnectionFactory.setCurrentRedis(moduleRateLimitRQ.getEnv());
        // 判断是否存在
        String key = RATE_SWITCH_PREFIX + moduleRateLimitRQ.getModule();
        if (moduleRateLimitRQ.getSaveOrNot() && Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return Boolean.FALSE;
        }
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        hashOps.put("enable", moduleRateLimitRQ.getEnable());
        hashOps.put("types", String.join(",", moduleRateLimitRQ.getTypes()));
        return Boolean.TRUE;
    }

    public List<ModuleRateLimitSwitchRS> loadRateLimitSwitch(String env, String module) {
        multiRedisConnectionFactory.setCurrentRedis(env);
        List<ModuleRateLimitSwitchRS> limitSwitchRS = new ArrayList<>();
        if (StrUtil.isEmpty(module)) {
            Set<String> keys = redisTemplate.keys(RateLimitService.RATE_SWITCH_PREFIX + "*");
            for (String key : keys) {
                Map<Object, Object> entry = redisTemplate.opsForHash().entries(key);
                String dbModule = key.split(":")[1];
                extractRateSwitchRs(dbModule, limitSwitchRS, entry);
            }
        } else {
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(RATE_CONFIG_PREFIX + module);
            Map<Object, Object> entry = hashOps.entries();
            extractRateSwitchRs(module, limitSwitchRS, entry);
        }
        return limitSwitchRS;
    }

    private void extractRateSwitchRs(String module, List<ModuleRateLimitSwitchRS> limitSwitchRS, Map<Object, Object> entry) {
        ModuleRateLimitSwitchRS rs = new ModuleRateLimitSwitchRS();
        rs.setEnable(Boolean.valueOf(((String) entry.get("enable"))));
        rs.setModule(module);
        rs.setTypes(Arrays.asList(((String) entry.get("types")).split(",")));
        limitSwitchRS.add(rs);
    }

    private void extractRateConfigRs(String module, List<ModuleRateLimitConfigRS> limitConfigRS, Map<Object, Object> entry, String type) {
        ModuleRateLimitConfigRS rs = new ModuleRateLimitConfigRS();
        rs.setModule(module);
        rs.setType(type);
        rs.setMax(((String) entry.get("max")));
        rs.setDuration(((String) entry.get("duration")));
        String configValue = (String) entry.get("configValue");
        if (StrUtil.isNotEmpty(configValue)) {
            rs.setConfigValue(configValue);
        }
        limitConfigRS.add(rs);
    }

    public void delRateLimitSwitch(String ids, String env) {
        multiRedisConnectionFactory.setCurrentRedis(env);
        String[] moduleArr = ids.split(",");
        for (String module : moduleArr) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(RATE_SWITCH_PREFIX + module))) {
                redisTemplate.delete(RATE_SWITCH_PREFIX + module);
            }
        }
    }

    public List<ModuleRateLimitConfigRS> loadRateLimitConfig(String env, String module) {
        multiRedisConnectionFactory.setCurrentRedis(env);
        List<ModuleRateLimitConfigRS> limitConfigRS = new ArrayList<>();
        Set<String> keys;
        if (StrUtil.isEmpty(module)) {
            keys = redisTemplate.keys(RateLimitService.RATE_CONFIG_PREFIX + "*");
        } else {
            keys = redisTemplate.keys(RateLimitService.RATE_CONFIG_PREFIX + module + "*");
        }
        for (String key : keys) {
            Map<Object, Object> entry = redisTemplate.opsForHash().entries(key);
            String dbModule = key.split(":")[1];
            String dbType = key.split(":")[2];
            extractRateConfigRs(dbModule, limitConfigRS, entry, dbType);
        }
        return limitConfigRS;
    }

    public boolean saveOrUpdateRateLimitConfig(ModuleRateLimitConfigRQ moduleRateLimitRQ) {
        multiRedisConnectionFactory.setCurrentRedis(moduleRateLimitRQ.getEnv());
        String key = RATE_CONFIG_PREFIX + moduleRateLimitRQ.getModule() + ":" + moduleRateLimitRQ.getType();
        if (StrUtil.isNotEmpty(moduleRateLimitRQ.getConfigValue())) {
            key = RATE_CONFIG_PREFIX + moduleRateLimitRQ.getModule() + ":" + moduleRateLimitRQ.getType() + ":" + moduleRateLimitRQ.getConfigValue();
        }
        if (moduleRateLimitRQ.getSaveOrNot() && Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return Boolean.FALSE;
        }
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        hashOps.put("duration", Long.valueOf(moduleRateLimitRQ.getDuration()));
        hashOps.put("max", Long.valueOf(moduleRateLimitRQ.getMax()));
        if (StrUtil.isNotEmpty(moduleRateLimitRQ.getConfigValue())) {
            hashOps.put("configValue", moduleRateLimitRQ.getConfigValue());
        }
        // 保存zset
        if (StrUtil.equals(moduleRateLimitRQ.getType(), "hot")) {
            // GET-/test:[{name1=value1},{name2=value2}]:AND:1
            String configValue = moduleRateLimitRQ.getConfigValue();
            String[] configValArr = configValue.split(":");
            String pathKey = configValArr[0];
            String value = configValArr[1];
            String judgeType = configValArr[2];
            String sort = configValArr[3];
            String zsetKey = RATE_LIMIT_HOT_PREFIX + moduleRateLimitRQ.getModule();
            BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(zsetKey);
            zSetOps.add(String.format("%s:%s:%s", pathKey, value, judgeType), Double.parseDouble(sort));
        }

        return Boolean.TRUE;
    }

    public void delRateLimitConfig(List<String> ids, String env) {
        multiRedisConnectionFactory.setCurrentRedis(env);
        for (String id : ids) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(RATE_CONFIG_PREFIX + id))) {
                redisTemplate.delete(RATE_CONFIG_PREFIX + id);
            }
            // 删除zset
            String[] idArr = id.split(":");
            if (ArrayUtil.length(idArr) > 4) {
                // ratelimit_test:hot:GET-/test:[{name1=value1},{name2=value2}]:AND:1
                String module = idArr[0];
                String pathKey = idArr[2];
                String value = idArr[3];
                String judgeType = idArr[4];
                String zsetKey = RATE_LIMIT_HOT_PREFIX + module;
                BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(zsetKey);
                zSetOps.remove(String.format("%s:%s:%s", pathKey, value, judgeType));
            }
        }
    }
}
