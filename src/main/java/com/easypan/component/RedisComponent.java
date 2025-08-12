package com.easypan.component;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SysSettingsDto;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils<SysSettingsDto> redisUtils;

    /**
     * 获取系统设置
     *
     * @return
     */
    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }
}
