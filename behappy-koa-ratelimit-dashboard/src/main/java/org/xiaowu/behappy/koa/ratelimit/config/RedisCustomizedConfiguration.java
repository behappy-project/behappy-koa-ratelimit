package org.xiaowu.behappy.koa.ratelimit.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xiaowu
 */
@ConditionalOnProperty(prefix = "spring.redis", value = "enable-multi", matchIfMissing = false)
@Configuration(proxyBeanMethods = false)
public class RedisCustomizedConfiguration {

    /**
     * @param multiRedisProperties
     * @return
     */
    @Bean
    public MultiRedisConnectionFactory multiRedisConnectionFactory(MultiRedisProperties multiRedisProperties) {
        Map<String, LettuceConnectionFactory> connectionFactoryMap = new HashMap<>();
        Map<String, RedisProperties> multi = multiRedisProperties.getMulti();
        multi.forEach((k, v) -> {
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setDatabase(v.getDatabase());
            redisStandaloneConfiguration.setHostName(v.getHost());
            redisStandaloneConfiguration.setPort(v.getPort());
            redisStandaloneConfiguration.setUsername(v.getUsername());
            redisStandaloneConfiguration.setPassword(RedisPassword.of(v.getPassword()));
            // @see org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
            LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration);
            connectionFactoryMap.put(k, lettuceConnectionFactory);
        });
        return new MultiRedisConnectionFactory(connectionFactoryMap);
    }

}
