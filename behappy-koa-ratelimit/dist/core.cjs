'use strict';

const Redis = require('ioredis');

const time = Date.now() * 1e3;
const start = process.hrtime.bigint();

const microtime = () => time + Number(process.hrtime.bigint() - start) * 1e-3;

async function getId(ctx, type, opts = {}) {
  // 【当type为qps时，duration应该配置为1000(1s)】
  switch (type) {
    case 'ip':
      return ctx.ip
    case 'qps':
      return {appName: opts}
    case 'interface':
      return `${ctx.method}-${ctx.path}`
    case 'hot':
      const params = {...ctx.query, ...ctx.request.body};
      let returnValue;
      for (const [configValue, sort] of Object.entries(opts.configValue)) {
        const configValueArr = configValue.split(':');
        const path = configValueArr[0];
        if (path !== `${ctx.method}-${ctx.path}`) {
          continue
        }
        const rule = configValueArr[1];
        const judgeType = configValueArr[2];
        // 提取条件
        const conditions = rule.match(/{(.+?)}/g).map(s => s.slice(1, -1));
        // 验证规则是否匹配
        let isValid = true;
        for (const condition of conditions) {
          const [key, val] = condition.split('=');
          if (judgeType === 'AND') {
            // 只要有一个不满足即直接返回false
            if (!(key in params) || params[key] != val) {
              isValid = false;
              break;
            }
          } else if (judgeType === 'OR') {
            // 只要有一个满足即直接返回true
            if (key in params && params[key] == val) {
              isValid = true;
              break;
            } else {
              isValid = false;
            }
          } else if (judgeType === 'NOT') {
            // 只要有一个满足匹配条件即直接返回false
            if (key in params && params[key] == val) {
              isValid = false;
              break;
            }
          }
        }
        if (isValid) {
          // 如果规则匹配上了，则直接返回key
          returnValue = `${configValue}:${sort}`;
          break
        }
      }
      return returnValue
    case 'error_code_num':
      if (opts.req) {
        return 'not_success'
      }
      const code = (ctx.response.body ? ctx.response.body.code : 1);
      return code === 0
    default:
      // 默认按照服务qps限流
      return {appName: opts}
  }
}

async function getHashField(redis, key, field) {
  return await redis.hget(key, field);
}

async function getAllHashField(redis, key) {
  return await redis.hgetall(key);
}

async function getZSet(redis, key) {
  const results = await redis.zrange(key, 0, -1, 'withscores');
  if (results && results.length > 0) {
    return results.reduce((acc, cur, i) => {
      if (i % 2 === 0) {
        acc[cur] = results[i + 1];
      }
      return acc;
    }, {});
  }
}

const RATE_CONFIG_PREFIX = 'BH-RATE-CONFIG:';
const RATE_SWITCH_PREFIX = 'BH-RATE-SWITCH:';
const RATE_LIMIT_PREFIX = 'BH-RATE-LIMIT:';
const RATE_LIMIT_HOT_PREFIX = 'BH-RATE-HOT:';

const rateLimit = function (options = {}) {
  const appName = options.appName;
  const cacheConfig = options.cacheConfig ?? {};
  const logEnable = options.logEnable ?? true;
  const rate_config_key = RATE_CONFIG_PREFIX + appName;
  const rate_switch_key = RATE_SWITCH_PREFIX + appName;
  const rate_limit_namespace = RATE_LIMIT_PREFIX + appName;
  const rate_limit_hot_key = RATE_LIMIT_HOT_PREFIX + appName;
  const db = new Redis({
    connectTimeout: 1000,
    retryStrategy(times) {
      return Math.min(times * 50, 3000);
    },
    host: cacheConfig.ratelimitCacheHost ?? '127.0.0.1',
    port: cacheConfig.ratelimitCachePort ?? 6379,
    password: cacheConfig.ratelimitCachePassword ?? '',
    db: cacheConfig.ratelimitCacheDb ?? 1,
  });
  db.on('error', err => {
    if (logEnable) {
      console.warn('koa rate limit error: ', err);
    }
  });
  return async (ctx, next) => {
    let types = [];
    let enable = false;
    let error_code_rate_config;
    const error_code_num_type = 'error_code_num';
    try {
      if (db.status !== 'ready') {
        return await next()
      }
      // 如果没开启或者压根不存在，直接next
      enable = await getHashField(db, rate_switch_key, 'enable');
      if (!enable || enable == 'false') {
        return await next()
      }
      // 判断每种限流类型下是否有剩余令牌
      types = (await getHashField(db, rate_switch_key, 'types')).split(",");
      for (const type of types) {
        let id;
        if (type === 'hot') {
          const configValue = await getZSet(db, rate_limit_hot_key);
          if (configValue){
            id = await getId(ctx, type, { appName, configValue });
          }else {
            continue
          }
        }else {
          id = await getId(ctx, type, { appName, req: true });
        }
        let rate_config;
        if (type === 'hot' && !id) {
          // 按热点参数 && 参数规则未匹配
          continue
        } else if (type === 'interface' || type === 'ip' || type === 'hot') {
          rate_config = await getAllHashField(db, `${rate_config_key}:${type}:${id}`);
        } else {
          rate_config = await getAllHashField(db, `${rate_config_key}:${type}`);
          if (type === error_code_num_type) error_code_rate_config = rate_config;
        }
        if (!rate_config || Object.keys(rate_config).length === 0) {
          continue
        }
        // redis操作
        const key = `${rate_limit_namespace}:${type}:${id}`;
        const duration = rate_config.duration;
        const max = rate_config.max;
        const count = await db.zcard(key);
        const remaining = count < max ? max - count : 0;
        if (!remaining) {
          ctx.status = 429;
          return ctx.body = `超过速率限制，请稍后再试.`
        }
        if (type !== error_code_num_type) {
          const now = microtime.now();
          const start = now - duration * 1000;
          const operations = [
            ['zremrangebyscore', key, 0, start],
            ['zadd', key, now, now],
            ['zremrangebyrank', key, 0, -(max + 1)],
            ['pexpire', key, duration]
          ];
          await db.multi(operations).exec();
        }
      }
    } catch (e) {
      if (logEnable) {
        console.error(e.message);
      }
    }
    await next();
    // 对于错位码的限流逻辑
    try {
      if (db.status !== 'ready') {
        return
      }
      if (enable == 'true'
        && types.includes(error_code_num_type)
        && error_code_rate_config
        && Object.keys(error_code_rate_config).length !== 0
      ) {
        const id = await getId(ctx, error_code_num_type, {appName});
        // 此处获取的是responseCode，如果code不为0，则记录redis
        if (!id) {
          const duration = error_code_rate_config.duration;
          const max = error_code_rate_config.max;
          const key = `${rate_limit_namespace}:${error_code_num_type}:not_success`;
          const now = microtime();
          const start = now - duration * 1000;
          const operations = [
            ['zremrangebyscore', key, 0, start],
            ['zadd', key, now, now],
            ['zremrangebyrank', key, 0, -(max + 1)],
            ['pexpire', key, duration]
          ];
          await db.multi(operations).exec();
        }
      }
    } catch (e) {
      if (logEnable) {
        console.error(e.message);
      }
    }
  };
};

module.exports = rateLimit;
//# sourceMappingURL=core.cjs.map
