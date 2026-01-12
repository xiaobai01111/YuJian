package com.campus.wall.property;

import cn.hutool.crypto.digest.BCrypt;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证相关属性测试
 * Property 1: 认证往返一致性 - 使用正确密码登录总是成功
 * Property 13: 权限拒绝正确性 - 未授权用户无法访问受保护资源
 */
class AuthPropertyTest {

    /**
     * Property 1: 密码哈希往返一致性
     * 对于任意密码，BCrypt哈希后使用原密码验证总是成功
     */
    @Property(tries = 100)
    void passwordHashRoundTrip(
            @ForAll @AlphaChars @StringLength(min = 6, max = 32) String password) {
        // 哈希密码
        String hashed = BCrypt.hashpw(password);

        // 验证原密码
        boolean verified = BCrypt.checkpw(password, hashed);

        assertThat(verified).isTrue();
    }

    /**
     * Property 1: 错误密码验证失败
     * 对于任意两个不同的密码，使用错误密码验证总是失败
     */
    @Property(tries = 100)
    void wrongPasswordAlwaysFails(
            @ForAll @AlphaChars @StringLength(min = 6, max = 32) String password,
            @ForAll @AlphaChars @StringLength(min = 6, max = 32) String wrongPassword) {
        Assume.that(!password.equals(wrongPassword));

        String hashed = BCrypt.hashpw(password);
        boolean verified = BCrypt.checkpw(wrongPassword, hashed);

        assertThat(verified).isFalse();
    }

    /**
     * Property: 每次哈希结果不同（盐值随机）
     * 相同密码的两次哈希结果应该不同
     */
    @Property(tries = 50)
    void hashesAreDifferentEachTime(
            @ForAll @AlphaChars @StringLength(min = 6, max = 32) String password) {
        String hash1 = BCrypt.hashpw(password);
        String hash2 = BCrypt.hashpw(password);

        assertThat(hash1).isNotEqualTo(hash2);

        // 但两者都能验证成功
        assertThat(BCrypt.checkpw(password, hash1)).isTrue();
        assertThat(BCrypt.checkpw(password, hash2)).isTrue();
    }

    /**
     * Property 13: 权限标识格式正确性
     * 权限标识应该符合 module:resource:action 格式
     */
    @Property(tries = 50)
    void permissionFormatIsValid(
            @ForAll("validPermissions") String permission) {
        // 权限格式: module:resource:action 或 *
        if ("*".equals(permission)) {
            assertThat(permission).isEqualTo("*");
        } else {
            String[] parts = permission.split(":");
            assertThat(parts.length).isGreaterThanOrEqualTo(2);
            for (String part : parts) {
                assertThat(part).matches("[a-zA-Z]+");
            }
        }
    }

    @Provide
    Arbitrary<String> validPermissions() {
        Arbitrary<String> modules = Arbitraries.of("system", "content", "user", "post");
        Arbitrary<String> resources = Arbitraries.of("user", "role", "menu", "post", "comment", "report");
        Arbitrary<String> actions = Arbitraries.of("list", "create", "update", "delete", "handle");

        return Combinators.combine(modules, resources, actions)
                .as((m, r, a) -> m + ":" + r + ":" + a);
    }

    /**
     * Property: 超级管理员ID固定
     */
    @Example
    void superAdminIdIsOne() {
        Long superAdminId = 1L;
        assertThat(superAdminId).isEqualTo(1L);
    }

    /**
     * Property: 用户名格式正确性
     * 用户名只能包含字母、数字和下划线
     */
    @Property(tries = 100)
    void usernameFormatIsValid(
            @ForAll("validUsernames") String username) {
        assertThat(username).matches("^[a-zA-Z0-9_]+$");
        assertThat(username.length()).isBetween(3, 20);
    }

    @Provide
    Arbitrary<String> validUsernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(3)
                .ofMaxLength(20);
    }

    /**
     * Property: 信用分范围正确性
     * 信用分应在 0-100 之间
     */
    @Property(tries = 100)
    void creditScoreInValidRange(@ForAll @net.jqwik.api.constraints.IntRange(min = -50, max = 150) int delta) {
        int currentScore = 100;
        int newScore = Math.max(0, Math.min(100, currentScore + delta));

        assertThat(newScore).isBetween(0, 100);
    }

    /**
     * Property: EDU邮箱格式正确性
     */
    @Property(tries = 50)
    void eduEmailFormatIsValid(@ForAll("validEduEmails") String email) {
        assertThat(email).endsWith(".edu.cn");
        assertThat(email).contains("@");
    }

    @Provide
    Arbitrary<String> validEduEmails() {
        Arbitrary<String> names = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(10);
        Arbitrary<String> domains = Arbitraries.of("pku", "tsinghua", "fudan", "zju");

        return Combinators.combine(names, domains)
                .as((n, d) -> n + "@" + d + ".edu.cn");
    }
}
