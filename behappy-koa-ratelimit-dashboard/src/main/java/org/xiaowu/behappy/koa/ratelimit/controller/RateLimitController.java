package org.xiaowu.behappy.koa.ratelimit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitConfigRQ;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitConfigRS;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitSwitchRQ;
import org.xiaowu.behappy.koa.ratelimit.pojo.ratelimit.ModuleRateLimitSwitchRS;
import org.xiaowu.behappy.koa.ratelimit.service.RateLimitService;

import java.util.List;
import java.util.Map;

/**
 * @author xiaowu
 */
@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    /**
     * @param env
     * @param module
     * @return
     */
    @GetMapping("/switch")
    public ResponseEntity<List<ModuleRateLimitSwitchRS>> loadRateLimitSwitch(@RequestParam String env,
                                                                             @RequestParam(required = false) String module){
        List<ModuleRateLimitSwitchRS> result = rateLimitService.loadRateLimitSwitch(env, module);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/switch")
    public ResponseEntity<?> saveOrUpdateRateLimitSwitch(@RequestBody @Valid ModuleRateLimitSwitchRQ moduleRateLimitRQ){
        boolean result = rateLimitService.saveOrUpdateRateLimitSwitch(moduleRateLimitRQ);
        if (result) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("配置已存在!");
    }

    @DeleteMapping("/switch")
    public ResponseEntity<?> delRateLimitSwitch(@RequestBody Map<String, Object> params){
        String ids = ((String) params.get("ids"));
        String env = ((String) params.get("env"));
        rateLimitService.delRateLimitSwitch(ids, env);
        return ResponseEntity.ok().build();
    }

    /**
     * @param env
     * @param module
     * @return
     */
    @GetMapping("/config")
    public ResponseEntity<List<ModuleRateLimitConfigRS>> loadRateLimitConfig(@RequestParam String env,
                                                                             @RequestParam(required = false) String module){
        List<ModuleRateLimitConfigRS> result = rateLimitService.loadRateLimitConfig(env, module);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/config")
    public ResponseEntity<?> saveOrUpdateRateLimitConfig(@RequestBody @Valid ModuleRateLimitConfigRQ moduleRateLimitRQ){
        boolean result = rateLimitService.saveOrUpdateRateLimitConfig(moduleRateLimitRQ);
        if (result) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("配置已存在!");
    }

    @DeleteMapping("/config")
    public ResponseEntity<?> delRateLimitConfig(@RequestBody Map<String, Object> params){
        List<String> ids = ((List) params.get("ids"));
        String env = ((String) params.get("env"));
        rateLimitService.delRateLimitConfig(ids, env);
        return ResponseEntity.ok().build();
    }

}
