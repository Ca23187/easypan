package com.easypan.infra.mapStruct;

import com.easypan.infra.jpa.entity.FileInfo;
import com.easypan.web.dto.response.FileInfoVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileInfoMapper {
    FileInfoVo toVo(FileInfo fileInfo);
}
