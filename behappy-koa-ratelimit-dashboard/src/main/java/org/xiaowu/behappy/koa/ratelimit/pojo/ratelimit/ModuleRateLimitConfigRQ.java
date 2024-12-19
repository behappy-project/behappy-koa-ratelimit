package org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author xiaowu
 */
@Data
public class ModuleRateLimitConfigRQ {

    @NotEmpty
    private String env;

    @NotEmpty
    private String module;

    @NotEmpty
    private String type;

    /**
     * 可能为空
     */
    private String configValue;

    @NotEmpty
    @Pattern(regexp = "^[0-9]*$")
    private String duration;

    @NotEmpty
    @Pattern(regexp = "^[0-9]*$")
    private String max;

    /**
     * 是否是新增
     */
    @NotNull
    private Boolean saveOrNot = Boolean.TRUE;
}
