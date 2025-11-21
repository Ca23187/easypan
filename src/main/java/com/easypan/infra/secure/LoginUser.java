package com.easypan.infra.secure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class LoginUser implements Serializable {
    private String userId;
    private String nickname;
    private Boolean isAdmin;
    private String avatar;
}
