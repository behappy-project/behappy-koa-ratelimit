package org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author xiaowu
 */
@Data
public class ModuleRateLimitSwitchRQ {

    @NotEmpty
    private String env;

    @NotEmpty
    private String module;

    @NotNull
    private Boolean enable;

    @NotEmpty
    private List<String> types;

    /**
     * 是否是新增
     */
    @NotNull
    private Boolean saveOrNot = Boolean.TRUE;

}
