package com.easypan.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class UserSpaceDto implements Serializable {
    private Long usedSpace;
    private Long totalSpace;
}
