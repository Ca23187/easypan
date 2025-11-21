package com.easypan.service;

public interface UserInfoService {
    void register(String email, String nickname, String password, String emailCode);

    String login(String email, String password);

    void resetPwd(String email, String password, String emailCode);

    void updateQqAvatarByUserId(String s, String userId);

    void updatePasswordByUserId(String s, String userId);

    String qqLogin(String code);
}
