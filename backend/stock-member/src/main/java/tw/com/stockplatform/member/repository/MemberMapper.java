package tw.com.stockplatform.member.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tw.com.stockplatform.domain.po.UserPO;

import java.util.Optional;

/**
 * USERS 表 MyBatis Mapper。
 * <p>
 * 規範：Repository → Service 可使用 Optional。
 * SQL 一律使用 #{} 防 SQL Injection；XML mapper Wave 1 暫不開（單表查詢註解夠用）。
 */
@Mapper
public interface MemberMapper {

    @Select("""
        SELECT user_id          AS userId,
               email,
               password_hash    AS passwordHash,
               display_name     AS displayName,
               status,
               email_verified_at AS emailVerifiedAt,
               created_at       AS createdAt,
               updated_at       AS updatedAt,
               deleted_at       AS deletedAt
          FROM users
         WHERE user_id = #{userId}
           AND deleted_at IS NULL
        """)
    Optional<UserPO> findById(@Param("userId") String userId);

    @Select("""
        SELECT user_id          AS userId,
               email,
               password_hash    AS passwordHash,
               display_name     AS displayName,
               status,
               email_verified_at AS emailVerifiedAt,
               created_at       AS createdAt,
               updated_at       AS updatedAt,
               deleted_at       AS deletedAt
          FROM users
         WHERE email = #{email}
           AND deleted_at IS NULL
        """)
    Optional<UserPO> findByEmail(@Param("email") String email);

    @Insert("""
        INSERT INTO users
            (user_id, email, password_hash, display_name, status,
             email_verified_at, created_at, updated_at, deleted_at)
        VALUES
            (#{userId}, #{email}, #{passwordHash}, #{displayName}, #{status},
             #{emailVerifiedAt}, #{createdAt}, #{updatedAt}, #{deletedAt})
        """)
    int insert(UserPO po);

    @Update("""
        UPDATE users
           SET display_name = #{displayName},
               updated_at   = #{updatedAt}
         WHERE user_id = #{userId}
           AND deleted_at IS NULL
        """)
    int updateDisplayName(@Param("userId") String userId,
                          @Param("displayName") String displayName,
                          @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
