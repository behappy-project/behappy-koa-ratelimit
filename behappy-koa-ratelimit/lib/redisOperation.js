const toNumber = str => parseInt(str, 10)

export async function getId(ctx, type, opts = {}) {
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
      let returnValue
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
          returnValue = `${configValue}:${sort}`
          break
        }
      }
      return returnValue
    case 'error_code_num':
      if (opts.req) {
        return 'not_success'
      }
      const code = (ctx.response.body ? ctx.response.body.code : 1)
      return code === 0
    default:
      // 默认按照服务qps限流
      return {appName: opts}
  }
}

export async function getHashField(redis, key, field) {
  return await redis.hget(key, field);
}

export async function getAllHashField(redis, key) {
  return await redis.hgetall(key);
}

export async function getZSet(redis, key) {
  const results = await redis.zrange(key, 0, -1, 'withscores');
  if (results && results.length > 0) {
    return results.reduce((acc, cur, i) => {
      if (i % 2 === 0) {
        acc[cur] = results[i + 1]
      }
      return acc;
    }, {});
  }
}
