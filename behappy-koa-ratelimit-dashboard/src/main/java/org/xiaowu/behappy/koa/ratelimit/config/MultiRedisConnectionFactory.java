package org.xiaowu.behappy.koa.ratelimit.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author xiaowu
 */
public class MultiRedisConnectionFactory
        implements InitializingBean, DisposableBean, RedisConnectionFactory, ReactiveRedisConnectionFactory {
    private final Map<String, LettuceConnectionFactory> connectionFactoryMap;

    /**
     * 当前redis的名字
     */
    private static final ThreadLocal<String> CURRENT_REDIS_NAME = new ThreadLocal<>();

    /**
     * 当前redis的db数据库
     */
    private static final ThreadLocal<Integer> CURRENT_REDIS_DB = new ThreadLocal<>();


    public MultiRedisConnectionFactory(Map<String, LettuceConnectionFactory> connectionFactoryMap) {
        this.connectionFactoryMap = connectionFactoryMap;
    }

    public void setCurrentRedis(String currentRedisName) {
        if (!connectionFactoryMap.containsKey(currentRedisName)) {
            throw new RuntimeException("invalid currentRedis: " + currentRedisName + ", it does not exist in configuration");
        }
        MultiRedisConnectionFactory.CURRENT_REDIS_NAME.set(currentRedisName);
    }

    /**
     * 选择连接和数据库
     *
     * @param currentRedisName
     * @param db
     */
    public void setCurrentRedis(String currentRedisName, Integer db) {
        if (!connectionFactoryMap.containsKey(currentRedisName)) {
            throw new RuntimeException("Invalid current redis: " + currentRedisName + ", it does not exist in configuration");
        }
        MultiRedisConnectionFactory.CURRENT_REDIS_NAME.set(currentRedisName);
        MultiRedisConnectionFactory.CURRENT_REDIS_DB.set(db);
    }


    @Override
    public void destroy() throws Exception {
        connectionFactoryMap.values().forEach(LettuceConnectionFactory::destroy);
        MultiRedisConnectionFactory.CURRENT_REDIS_NAME.remove();
        MultiRedisConnectionFactory.CURRENT_REDIS_DB.remove();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connectionFactoryMap.values().forEach(LettuceConnectionFactory::afterPropertiesSet);
    }

    private LettuceConnectionFactory currentLettuceConnectionFactory() {
        String currentRedisName = MultiRedisConnectionFactory.CURRENT_REDIS_NAME.get();
        if (!ObjectUtils.isEmpty(currentRedisName)) {
            return connectionFactoryMap.get(currentRedisName);
        }
        return connectionFactoryMap.get(MultiRedisProperties.DEFAULT);
    }

    @Override
    public ReactiveRedisConnection getReactiveConnection() {
        return currentLettuceConnectionFactory().getReactiveConnection();
    }

    @Override
    public ReactiveRedisClusterConnection getReactiveClusterConnection() {
        return currentLettuceConnectionFactory().getReactiveClusterConnection();
    }

    @Override
    public RedisConnection getConnection() {
        // 切换数据库
        Integer currentRedisDb = MultiRedisConnectionFactory.CURRENT_REDIS_DB.get();
        if (!ObjectUtils.isEmpty(currentRedisDb)) {
            LettuceConnectionFactory lettuceConnectionFactory = currentLettuceConnectionFactory();
            lettuceConnectionFactory.setShareNativeConnection(false);
            RedisConnection connection = lettuceConnectionFactory.getConnection();
            connection.select(currentRedisDb);
            return connection;
        }
        return currentLettuceConnectionFactory().getConnection();
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        return currentLettuceConnectionFactory().getClusterConnection();
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return currentLettuceConnectionFactory().getConvertPipelineAndTxResults();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return currentLettuceConnectionFactory().getSentinelConnection();
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return currentLettuceConnectionFactory().translateExceptionIfPossible(ex);
    }

}
