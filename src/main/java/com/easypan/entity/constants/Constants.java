package com.easypan.entity.constants;

import java.util.concurrent.TimeUnit;

public class Constants {
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    public static final Integer CHECK_CODE_SIZE = 5;
    public static final Integer EMAIL_CODE_SIZE = 5;

    public static final TimeUnit REDIS_TIME_UNIT_CHECK_CODE = TimeUnit.MINUTES;
    public static final Long REDIS_EXPIRATION_CHECK_CODE = 5L;
    public static final Long REDIS_EXPIRATION_EMAIL_CODE = 15L;

    public static final TimeUnit REDIS_TIME_UNIT_USER_SPACE = TimeUnit.HOURS;
    public static final Long REDIS_EXPIRATION_USER_SPACE = 1L;

    public static final String REDIS_KEY_SYS_SETTING = "easypan:sys:setting:";

    public static final String REDIS_KEY_CHECK_CODE_PREFIX = "easypan:auth:check-code:";
    public static final String REDIS_KEY_CHECK_CODE_FOR_EMAIL_PREFIX = "easypan:auth:check-code:email:";
    public static final String REDIS_KEY_EMAIL_CODE_PREFIX = "easypan:auth:email-code:";

    public static final String REDIS_KEY_USER_SPACE_USE = "easypan:user:spaceuse:";

    public static final String FILE_FOLDER_FILE = "/file/";
    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";

    public static final String AVATAR_SUFFIX = ".jpg";
    public static final String DEFAULT_AVATAR_NAME = "default_avatar.jpg";

    public static final Long MB = 1024 * 1024L;

}
