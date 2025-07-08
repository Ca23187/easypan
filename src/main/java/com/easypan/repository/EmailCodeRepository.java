package com.easypan.repository;

import com.easypan.entity.po.EmailCode;
import com.easypan.entity.po.EmailCodeId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailCodeRepository extends JpaRepository<EmailCode, EmailCodeId> {
    @Transactional
    @Modifying
    @Query("UPDATE EmailCode e SET e.status = 1 WHERE e.id.email = :email AND e.status = 0")
    int disableEmailCode(@Param("email") String email);
}
