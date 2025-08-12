package com.easypan.repository;

import com.easypan.entity.po.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoRepository extends JpaRepository<UserInfo, String> {
    UserInfo findByEmail(String email);
    UserInfo findByEmailOrNickname(String email, String nickname);
}
