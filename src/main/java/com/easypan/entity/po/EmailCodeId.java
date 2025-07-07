package com.easypan.entity.po;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EmailCodeId implements Serializable {
    private String email;
    private String code;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        EmailCodeId emailCodeId = (EmailCodeId) o;
            return getEmail() != null && getCode() != null
                    && Objects.equals(getEmail(), emailCodeId.getEmail())
                    && Objects.equals(getCode(), emailCodeId.getCode());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(email, code);
    }
}
