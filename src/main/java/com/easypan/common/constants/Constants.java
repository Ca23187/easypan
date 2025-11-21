package com.easypan.common.constants;

import java.util.concurrent.TimeUnit;

public final class Constants {
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    public static final Integer CHECK_CODE_LENGTH = 5;
    public static final Integer EMAIL_CODE_LENGTH = 5;
    public static final Integer QQ_LOGIN_STATE_LENGTH = 30;

    public static final TimeUnit REDIS_TIME_UNIT_CHECK_CODE = TimeUnit.MINUTES;
    public static final Long REDIS_EXPIRATION_CHECK_CODE = 5L;
    public static final Long REDIS_EXPIRATION_EMAIL_CODE = 15L;

    public static final TimeUnit REDIS_TIME_UNIT_USER_SPACE_INFO = TimeUnit.DAYS;
    public static final Long REDIS_EXPIRATION_USER_SPACE_INFO = 1L;

    public static final TimeUnit REDIS_TIME_UNIT_QQ_LOGIN = TimeUnit.MINUTES;
    public static final Long REDIS_EXPIRATION_QQ_LOGIN = 5L;

    public static final String REDIS_KEY_SYS_SETTING = "easypan:sys:setting:";
    public static final String REDIS_KEY_TOKEN_BLACKLIST = "easypan:sys:blacklist:";

    public static final String REDIS_KEY_CAPTCHA = "easypan:auth:captcha:";
    public static final String REDIS_KEY_CAPTCHA_FOR_EMAIL = "easypan:auth:captcha:email:";
    public static final String REDIS_KEY_EMAIL_CODE = "easypan:auth:email-code:";
    public static final String REDIS_KEY_QQ_LOGIN_STATE = "easypan:auth:qq:login-state:";
    public static final String REDIS_KEY_USER_SPACE_INFO = "easypan:user:space-info:";
    public static final String REDIS_KEY_USER_TEMP_FILE_CURRENT_SIZE = "easypan:user:file:temp:";

    public static final String FILE_FOLDER_FILE = "/file/";
    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";

    public static final String AVATAR_SUFFIX = ".jpg";
    public static final String DEFAULT_AVATAR_NAME = "default_avatar.jpg";

    public static final Long MB = 1024 * 1024L;

    public static final String VIEW_OBJ_RESULT_KEY = "result";

    public static final Integer PAGE_SIZE = 15;

    public static final Integer RANDOM_FILE_ID_LENGTH = 10;

    public static final String FILE_FOLDER_TEMP = "/temp/";

    public static final TimeUnit REDIS_TIME_UNIT_TEMP_FILE_CURRENT_SIZE = TimeUnit.HOURS;
    public static final Long REDIS_EXPIRATION_TEMP_FILE_CURRENT_SIZE = 1L;

    public static final String IMAGE_PNG_SUFFIX = ".png";
    public static final Integer THUMBNAIL_WIDTH = 150;

    public static final String TS_NAME = "index.ts";
    public static final String M3U8_NAME = "index.m3u8";
    public static final Integer EMAIL_CODE_TYPE_REGISTER = 0;
}
