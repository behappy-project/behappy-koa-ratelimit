package org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit;

import lombok.Data;

import java.util.List;

/**
 * @author xiaowu
 */
@Data
public class ModuleRateLimitSwitchRS {

    private String module;

    private Boolean enable;

    private List<String> types;

}
