package org.xiaowu.behappy.koa.ratelimit.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author xiaowu
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class MultiRedisProperties {
    /**
     * 默认连接必须配置，配置 key 为 default
     */
    public static final String DEFAULT = "default";

    private boolean enableMulti = false;


    private Map<String, RedisProperties> multi;
}
