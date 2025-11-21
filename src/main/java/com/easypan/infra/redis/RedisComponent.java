package com.easypan.infra.redis;

import com.easypan.common.constants.Constants;
import com.easypan.service.dto.SysSettingsDto;
import com.easypan.service.dto.UserSpaceDto;
import com.easypan.infra.jpa.repository.FileInfoRepository;
import jakarta.annotation.Resource;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils<Object> redisUtils;

    @Resource
    private FileInfoRepository fileInfoRepository;

    /**
     * 获取系统设置
     *
     */
    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    /**
     * 获取用户使用的空间
     *
     */
    public UserSpaceDto getUserSpaceInfo(String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_INFO + userId);
        if (null == userSpaceDto) {
            userSpaceDto = fileInfoRepository.findUserSpaceDtoByUserId(userId);
            redisUtils.setex(
                    Constants.REDIS_KEY_USER_SPACE_INFO + userId,
                    userSpaceDto,
                    Constants.REDIS_EXPIRATION_USER_SPACE_INFO,
                    Constants.REDIS_TIME_UNIT_USER_SPACE_INFO
            );
        }
        return userSpaceDto;
    }

    /**
     * 保存已使用的空间
     *
     */
    public void saveUserSpaceInfo(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(
                Constants.REDIS_KEY_USER_SPACE_INFO + userId,
                userSpaceDto,
                Constants.REDIS_EXPIRATION_USER_SPACE_INFO,
                Constants.REDIS_TIME_UNIT_USER_SPACE_INFO
        );
    }

    public Long getTempFileCurrentSize(String userId, String fileId) {
        String key = Constants.REDIS_KEY_USER_TEMP_FILE_CURRENT_SIZE + userId + fileId;
        Object sizeObj = redisUtils.get(key);
        return (sizeObj instanceof Number) ? ((Number) sizeObj).longValue() : 0L;
    }

    public void updateTempFileCurrentSize(String userId, String fileId, Long fileSize) {
        String key = Constants.REDIS_KEY_USER_TEMP_FILE_CURRENT_SIZE + userId + fileId;
        Long newSize = redisUtils.incrByWithExpire(
                key,
                fileSize,
                Constants.REDIS_EXPIRATION_TEMP_FILE_CURRENT_SIZE,
                Constants.REDIS_TIME_UNIT_TEMP_FILE_CURRENT_SIZE
        );
        if (newSize == null) {
            LoggerFactory.getLogger(RedisComponent.class)
                    .warn("更新临时文件大小失败, key={}, delta={}", key, fileSize);
        }
    }
}
