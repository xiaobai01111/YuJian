package com.campus.wall.config;

import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.util.SensitiveFieldUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 启动时回填敏感字段密文与哈希，兼容历史明文数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveDataBackfillRunner implements ApplicationRunner {

    private static final int BATCH_SIZE = 200;

    private static final String SELECT_USERS_TO_BACKFILL = """
        SELECT id, email, edu_email, phone, student_id
        FROM users
        WHERE id > ?
          AND (
            (email IS NOT NULL AND email <> '' AND (email_hash IS NULL OR email NOT LIKE 'ENCv1:%'))
            OR (edu_email IS NOT NULL AND edu_email <> '' AND (edu_email_hash IS NULL OR edu_email NOT LIKE 'ENCv1:%'))
            OR (phone IS NOT NULL AND phone <> '' AND (phone_hash IS NULL OR phone NOT LIKE 'ENCv1:%'))
            OR (student_id IS NOT NULL AND student_id <> '' AND (student_id_hash IS NULL OR student_id NOT LIKE 'ENCv1:%'))
          )
        ORDER BY id ASC
        LIMIT ?
        """;

    private static final String SELECT_VERIFICATIONS_TO_BACKFILL = """
        SELECT id, student_id
        FROM identity_verifications
        WHERE id > ?
          AND student_id IS NOT NULL
          AND student_id <> ''
          AND (student_id_hash IS NULL OR student_id NOT LIKE 'ENCv1:%')
        ORDER BY id ASC
        LIMIT ?
        """;

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final IdentityVerificationMapper verificationMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int userCount = backfillUsers();
            int verificationCount = backfillVerifications();
            if (userCount > 0 || verificationCount > 0) {
                log.info("Sensitive data backfill completed: users={}, identityVerifications={}", userCount, verificationCount);
            }
        } catch (Exception ex) {
            log.warn("Sensitive data backfill skipped: {}", ex.getMessage());
        }
    }

    private int backfillUsers() {
        int updated = 0;
        long lastId = 0L;
        while (true) {
            List<UserSensitiveRow> rows = jdbcTemplate.query(
                SELECT_USERS_TO_BACKFILL,
                (rs, rowNum) -> mapUserRow(rs),
                lastId,
                BATCH_SIZE
            );
            if (rows == null || rows.isEmpty()) {
                return updated;
            }
            for (UserSensitiveRow row : rows) {
                lastId = Math.max(lastId, row.id());
                try {
                    User patch = new User();
                    patch.setId(row.id());
                    boolean shouldUpdate = false;

                    if (StringUtils.hasText(row.email())) {
                        String email = SensitiveFieldUtil.normalizeEmail(SensitiveFieldUtil.decryptIfNeeded(row.email()));
                        patch.setEmail(email);
                        patch.setEmailHash(SensitiveFieldUtil.hashEmail(email));
                        shouldUpdate = true;
                    }
                    if (StringUtils.hasText(row.eduEmail())) {
                        String eduEmail = SensitiveFieldUtil.normalizeEmail(SensitiveFieldUtil.decryptIfNeeded(row.eduEmail()));
                        patch.setEduEmail(eduEmail);
                        patch.setEduEmailHash(SensitiveFieldUtil.hashEmail(eduEmail));
                        shouldUpdate = true;
                    }
                    if (StringUtils.hasText(row.phone())) {
                        String phone = SensitiveFieldUtil.normalizePhone(SensitiveFieldUtil.decryptIfNeeded(row.phone()));
                        patch.setPhone(phone);
                        patch.setPhoneHash(SensitiveFieldUtil.hashPhone(phone));
                        shouldUpdate = true;
                    }
                    if (StringUtils.hasText(row.studentId())) {
                        String studentId = SensitiveFieldUtil.normalizeStudentId(SensitiveFieldUtil.decryptIfNeeded(row.studentId()));
                        patch.setStudentId(studentId);
                        patch.setStudentIdHash(SensitiveFieldUtil.hashStudentId(studentId));
                        shouldUpdate = true;
                    }

                    if (shouldUpdate) {
                        userMapper.updateById(patch);
                        updated++;
                    }
                } catch (Exception ex) {
                    log.warn("Sensitive backfill failed for user id={}: {}", row.id(), ex.getMessage());
                }
            }
            if (rows.size() < BATCH_SIZE) {
                return updated;
            }
        }
    }

    private int backfillVerifications() {
        int updated = 0;
        long lastId = 0L;
        while (true) {
            List<VerificationSensitiveRow> rows = jdbcTemplate.query(
                SELECT_VERIFICATIONS_TO_BACKFILL,
                (rs, rowNum) -> mapVerificationRow(rs),
                lastId,
                BATCH_SIZE
            );
            if (rows == null || rows.isEmpty()) {
                return updated;
            }
            for (VerificationSensitiveRow row : rows) {
                lastId = Math.max(lastId, row.id());
                try {
                    String studentId = SensitiveFieldUtil.normalizeStudentId(SensitiveFieldUtil.decryptIfNeeded(row.studentId()));
                    if (!StringUtils.hasText(studentId)) {
                        continue;
                    }
                    IdentityVerification patch = new IdentityVerification();
                    patch.setId(row.id());
                    patch.setStudentId(studentId);
                    patch.setStudentIdHash(SensitiveFieldUtil.hashStudentId(studentId));
                    verificationMapper.updateById(patch);
                    updated++;
                } catch (Exception ex) {
                    log.warn("Sensitive backfill failed for verification id={}: {}", row.id(), ex.getMessage());
                }
            }
            if (rows.size() < BATCH_SIZE) {
                return updated;
            }
        }
    }

    private UserSensitiveRow mapUserRow(ResultSet rs) throws SQLException {
        return new UserSensitiveRow(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("edu_email"),
            rs.getString("phone"),
            rs.getString("student_id")
        );
    }

    private VerificationSensitiveRow mapVerificationRow(ResultSet rs) throws SQLException {
        return new VerificationSensitiveRow(
            rs.getLong("id"),
            rs.getString("student_id")
        );
    }

    private record UserSensitiveRow(Long id, String email, String eduEmail, String phone, String studentId) {
    }

    private record VerificationSensitiveRow(Long id, String studentId) {
    }
}
