import type {Middleware} from 'koa';

interface Options {
    appName: string;
    logEnable?: boolean;
    cacheConfig?: CacheConfig;
}

interface CacheConfig {
    ratelimitCachePort?: number;
    ratelimitCacheHost?: string;
    ratelimitCachePassword?: string;
    ratelimitCacheDb?: number;
}

declare function rateLimit(options: Options): Middleware;
export default rateLimit
