package com.easypan.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DISABLE(0, "禁用"),
    ENABLE(1, "启用");


    private final Integer status;
    private final String desc;

    public static UserStatusEnum getByStatus(Integer status) {
        for (UserStatusEnum item : UserStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item;
            }
        }
        return null;
    }

}
