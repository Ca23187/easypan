package com.easypan.infra.jpa.repository;

import com.easypan.infra.jpa.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserInfoRepository extends JpaRepository<UserInfo, String> {
    UserInfo findByEmail(String email);
    boolean existsByEmail(String email);
    UserInfo findByEmailOrNickname(String email, String nickname);

    @Query("update UserInfo u set u.qqAvatar = :qqAvatar where u.userId = :userId")
    @Modifying
    void updateQqAvatarByUserId(String qqAvatar, String userId);

    @Query("update UserInfo u set u.password = :password where u.userId = :userId")
    @Modifying
    void updatePasswordByUserId(String password, String userId);

    UserInfo findByQqOpenId(String qqOpenId);

    // FIXME: 这个查询只在 usedSpace 或 totalSpace 其中一个为 null 的时候正确，需要注意
    @Modifying
    @Query(value = """
        UPDATE user_info
        SET
            used_space = CASE WHEN :usedSpace IS NOT NULL THEN used_space + :usedSpace ELSE used_space END,
            total_space = CASE WHEN :totalSpace IS NOT NULL THEN total_space + :totalSpace ELSE total_space END
        WHERE user_id = :userId
          AND (:usedSpace IS NULL OR (used_space + :usedSpace) <= total_space)
          AND (:totalSpace IS NULL OR (total_space + :totalSpace) >= used_space)
        """, nativeQuery = true)
    int updateUserSpaceInfoByUserId(String userId, Long usedSpace, Long totalSpace);
}
