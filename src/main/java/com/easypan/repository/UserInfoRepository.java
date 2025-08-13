package com.easypan.repository;

import com.easypan.entity.po.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;

public interface UserInfoRepository extends JpaRepository<UserInfo, String> {
    UserInfo findByEmail(String email);
    UserInfo findByEmailOrNickname(String email, String nickname);

    @Query("update UserInfo u set u.lastLoginTime = :lastLoginTime where u.userId = :userId")
    @Modifying
    void updateLastLoginTimeByUserId(Date lastLoginTime, String userId);

    @Query("update UserInfo u set u.qqAvatar = :qqAvatar where u.userId = :userId")
    @Modifying
    void updateQqAvatarByUserId(String qqAvatar, String userId);

    @Query("update UserInfo u set u.password = :password where u.userId = :userId")
    @Modifying
    void updatePasswordByUserId(String password, String userId);
}
