package com.easypan.infra.jpa.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息
 */
@Getter
@Setter
@Entity
@Table
public class UserInfo implements Serializable {

    /**
     * 用户ID
     */
    @Id
    private String userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * qq openID
     */
    private String qqOpenId;

    /**
     * qq 头像
     */
    private String qqAvatar;


    /**
     * 密码
     */
    private String password;

    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime joinTime;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastLoginTime;

    /**
     * 0:禁用 1:正常
     */
    private Integer status;

    /**
     * 使用空间单位byte
     */
    private Long usedSpace;

    /**
     * 总空间单位byte
     */
    private Long totalSpace;

}
