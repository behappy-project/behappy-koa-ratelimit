package org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit;

import lombok.Data;

/**
 * @author xiaowu
 */
@Data
public class ModuleRateLimitConfigRS {

    private String module;

    private String type;

    private String duration;

    private String max;

    private String configValue;
}
