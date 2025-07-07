package com.easypan.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor  // JPA实体类必须添加，需要注意
@Entity
@Table
public class EmailCode implements Serializable {

    /**
     * JPA的复合主键定义方式，需要把复合主键单独封装进一个类中
     */
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

    /**
     * JPA不推荐使用@Data重写equals和hashcode，因此只能这样写了
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        EmailCode emailCode = (EmailCode) o;
        return getId() != null && Objects.equals(getId(), emailCode.getId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
