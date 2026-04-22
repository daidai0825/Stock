package tw.com.stockplatform.member.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tw.com.stockplatform.domain.po.UserPreferencePO;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface UserPreferenceMapper {

    @Select("""
        SELECT preference_id          AS preferenceId,
               user_id                AS userId,
               preset_type            AS presetType,
               tech_weight            AS techWeight,
               chip_weight            AS chipWeight,
               fund_weight            AS fundWeight,
               risk_weight            AS riskWeight,
               news_weight            AS newsWeight,
               notify_email_enabled   AS notifyEmailEnabled,
               notify_web_enabled     AS notifyWebEnabled,
               notify_telegram_enabled AS notifyTelegramEnabled,
               telegram_chat_id       AS telegramChatId,
               fcm_token              AS fcmToken,
               created_at             AS createdAt,
               updated_at             AS updatedAt
          FROM user_preferences
         WHERE user_id = #{userId}
        """)
    Optional<UserPreferencePO> findByUserId(@Param("userId") String userId);

    @Insert("""
        INSERT INTO user_preferences
            (preference_id, user_id, preset_type,
             tech_weight, chip_weight, fund_weight, risk_weight, news_weight,
             notify_email_enabled, notify_web_enabled, notify_telegram_enabled,
             telegram_chat_id, fcm_token,
             created_at, updated_at)
        VALUES
            (#{preferenceId}, #{userId}, #{presetType},
             #{techWeight}, #{chipWeight}, #{fundWeight}, #{riskWeight}, #{newsWeight},
             #{notifyEmailEnabled}, #{notifyWebEnabled}, #{notifyTelegramEnabled},
             #{telegramChatId}, #{fcmToken},
             #{createdAt}, #{updatedAt})
        """)
    int insert(UserPreferencePO po);

    @Update("""
        UPDATE user_preferences
           SET notify_email_enabled    = #{notifyEmailEnabled},
               notify_web_enabled      = #{notifyWebEnabled},
               notify_telegram_enabled = #{notifyTelegramEnabled},
               updated_at              = #{updatedAt}
         WHERE user_id = #{userId}
        """)
    int updateNotifyFlags(@Param("userId") String userId,
                          @Param("notifyEmailEnabled") String notifyEmailEnabled,
                          @Param("notifyWebEnabled") String notifyWebEnabled,
                          @Param("notifyTelegramEnabled") String notifyTelegramEnabled,
                          @Param("updatedAt") LocalDateTime updatedAt);
}
