package com.easypan.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Entity
@Table
public class EmailCode implements Serializable {

    // NOTE: JPA的复合主键定义方式，需要把复合主键单独封装进一个类中
    @EmbeddedId
    private EmailCodeId id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    /**
     * 0:未使用  1:已使用
     */
    private Integer status;

}
