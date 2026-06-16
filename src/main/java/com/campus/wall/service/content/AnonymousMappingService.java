package com.campus.wall.service.content;

/**
 * 匿名映射服务接口
 * 用于树洞等匿名场景的用户身份加密存储
 */
public interface AnonymousMappingService {

    /**
     * 加密用户ID
     * @param userId 用户ID
     * @param context 上下文（如帖子ID）
     * @return 加密后的映射ID
     */
    String encryptUserId(Long userId, String context);

    /**
     * 解密获取用户ID（需要管理员权限）
     * @param encryptedId 加密的映射ID
     * @param context 上下文
     * @return 原始用户ID
     */
    Long decryptUserId(String encryptedId, String context);

    /**
     * 生成匿名标识
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 匿名标识（如"匿名用户A"）
     */
    String generateAnonymousTag(Long userId, Long postId);

    /**
     * 验证匿名用户是否为同一人
     * @param encryptedId1 加密ID1
     * @param encryptedId2 加密ID2
     * @param context 上下文
     * @return 是否为同一用户
     */
    boolean isSameUser(String encryptedId1, String encryptedId2, String context);
}
