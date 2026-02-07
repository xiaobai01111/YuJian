package com.campus.wall.service.system;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.constant.CacheConstants;
import com.campus.wall.dto.system.OnlineUserQueryDTO;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.impl.OnlineUserServiceImpl;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.vo.system.OnlineUserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlineUserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private OnlineUserServiceImpl onlineUserService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void queryOnlineUsers_returnsCurrentUser() {
        StpUtil.login(1L);

        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setNickname("Tester");
        user.setLoginDate(LocalDateTime.now());

        when(userMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Long.valueOf(1L).equals(id) ? user : null;
        });
        when(setOperations.size(CacheConstants.ONLINE_TOKENS)).thenReturn(0L);

        OnlineUserQueryDTO query = new OnlineUserQueryDTO();
        query.setPage(1);
        query.setSize(10);

        PageResult<OnlineUserVO> result = onlineUserService.queryOnlineUsers(query);

        assertThat(result.getRecords()).isNotEmpty();
        assertThat(result.getTotal()).isGreaterThanOrEqualTo(1);
        assertThat(result.getRecords()).anySatisfy(vo -> assertThat(vo.getUserId()).isEqualTo(1L));
    }

    @Test
    void kickoutByToken_removesRedisToken() {
        StpUtil.login(2L);
        String token = StpUtil.getTokenValue();

        when(setOperations.remove(CacheConstants.ONLINE_TOKENS, token)).thenReturn(1L);

        onlineUserService.kickoutByToken(token);

        verify(setOperations).remove(CacheConstants.ONLINE_TOKENS, token);
    }
}
