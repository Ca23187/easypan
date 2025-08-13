package com.easypan.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto implements Serializable {
    private String userId;
    private String nickName;
    private Boolean isAdmin;
    private String avatar;
}
