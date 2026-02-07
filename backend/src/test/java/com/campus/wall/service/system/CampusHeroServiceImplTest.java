package com.campus.wall.service.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.campus.CampusHeroDTO;
import com.campus.wall.entity.system.CampusHero;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.system.CampusHeroMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.impl.CampusHeroServiceImpl;
import com.campus.wall.vo.campus.CampusHeroVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusHeroServiceImplTest {

    @Mock
    private CampusHeroMapper campusHeroMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private CampusHeroServiceImpl campusHeroService;

    @Test
    void query_normalizesPageAndSize() {
        CampusHero hero = hero(1L, "HOME", true);
        Page<CampusHero> page = new Page<>(1, 1);
        page.setRecords(List.of(hero));
        page.setTotal(1L);
        when(campusHeroMapper.selectPage(
            org.mockito.ArgumentMatchers.<Page<CampusHero>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<CampusHero>>any()
        )).thenReturn(page);

        var result = campusHeroService.query(0, 0, null, null);

        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void getById_missing_throwsNotFound() {
        when(campusHeroMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> campusHeroService.getById(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void create_blankPageKey_throwsBadRequest() {
        CampusHeroDTO dto = new CampusHeroDTO();
        dto.setPageKey(" ");

        assertThatThrownBy(() -> campusHeroService.create(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void create_duplicatePageKey_throwsBadRequest() {
        CampusHeroDTO dto = new CampusHeroDTO();
        dto.setPageKey("home");
        when(campusHeroMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> campusHeroService.create(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("页面标识已存在");
    }

    @Test
    void create_success_appliesDefaults() {
        CampusHeroDTO dto = new CampusHeroDTO();
        dto.setPageKey("home");
        dto.setPageName("首页");
        dto.setTitleStart("欢迎");
        when(campusHeroMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            CampusHero hero = invocation.getArgument(0);
            hero.setId(100L);
            return 1;
        }).when(campusHeroMapper).insert(any(CampusHero.class));

        CampusHeroVO vo = campusHeroService.create(dto);

        assertThat(vo.getId()).isEqualTo(100L);
        assertThat(vo.getPageKey()).isEqualTo("HOME");
        assertThat(vo.getEnabled()).isTrue();
        assertThat(vo.getShowStats()).isTrue();
        assertThat(vo.getSortOrder()).isEqualTo(0);
    }

    @Test
    void update_missing_throwsNotFound() {
        when(campusHeroMapper.selectById(5L)).thenReturn(null);
        CampusHeroDTO dto = new CampusHeroDTO();
        dto.setPageKey("home");

        assertThatThrownBy(() -> campusHeroService.update(5L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void update_duplicatePageKey_throwsBadRequest() {
        CampusHero existing = hero(5L, "HOME", true);
        when(campusHeroMapper.selectById(5L)).thenReturn(existing);
        when(campusHeroMapper.selectCount(any())).thenReturn(1L);
        CampusHeroDTO dto = new CampusHeroDTO();
        dto.setPageKey("MARKET");

        assertThatThrownBy(() -> campusHeroService.update(5L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("页面标识已存在");
    }

    @Test
    void update_success_updatesFields() {
        CampusHero existing = hero(5L, "HOME", true);
        existing.setPageName("旧");
        when(campusHeroMapper.selectById(5L)).thenReturn(existing);
        when(campusHeroMapper.selectCount(any())).thenReturn(0L);
        CampusHeroDTO dto = new CampusHeroDTO();
        dto.setPageKey("market");
        dto.setPageName("市集");
        dto.setEnabled(false);

        CampusHeroVO vo = campusHeroService.update(5L, dto);

        assertThat(vo.getPageKey()).isEqualTo("MARKET");
        assertThat(vo.getPageName()).isEqualTo("市集");
        assertThat(vo.getEnabled()).isFalse();
        verify(campusHeroMapper).updateById(existing);
    }

    @Test
    void getByPageKey_nullOrMissing_returnsNull() {
        assertThat(campusHeroService.getByPageKey(null)).isNull();
        when(campusHeroMapper.selectOne(any())).thenReturn(null);
        assertThat(campusHeroService.getByPageKey("home")).isNull();
    }

    @Test
    void getByPageKey_showStatsFalse_clearsStatsAndAvatars() {
        CampusHero hero = hero(1L, "HOME", true);
        hero.setShowStats(false);
        hero.setStatsNumber("123");
        hero.setStatsLabel("x");
        hero.setAvatarUrls(List.of("a"));
        when(campusHeroMapper.selectOne(any())).thenReturn(hero);

        CampusHeroVO vo = campusHeroService.getByPageKey("home");

        assertThat(vo).isNotNull();
        assertThat(vo.getStatsNumber()).isNull();
        assertThat(vo.getStatsLabel()).isNull();
        assertThat(vo.getAvatarUrls()).isEmpty();
        assertThat(vo.getAvatarNames()).isEmpty();
    }

    @Test
    void getByPageKey_home_statsAndAvatarsCalculated() {
        CampusHero hero = hero(1L, "HOME", true);
        hero.setShowStats(true);
        when(campusHeroMapper.selectOne(any())).thenReturn(hero);
        when(userMapper.selectCount(any())).thenReturn(1234L);
        User u1 = new User();
        u1.setAvatar("a1");
        u1.setNickname("Nick1");
        User u2 = new User();
        u2.setAvatar("a2");
        u2.setUsername("user2");
        when(userMapper.selectList(any())).thenReturn(List.of(u1, u2));

        CampusHeroVO vo = campusHeroService.getByPageKey("home");

        assertThat(vo.getStatsNumber()).isEqualTo("1,234+");
        assertThat(vo.getStatsLabel()).isEqualTo("同学已加入");
        assertThat(vo.getAvatarUrls()).containsExactly("a1", "a2");
        assertThat(vo.getAvatarNames()).containsExactly("Nick1", "user2");
    }

    @Test
    void getByPageKey_unknownPage_usesPostCountAndDefaultLabel() {
        CampusHero hero = hero(1L, "CUSTOM", true);
        hero.setShowStats(true);
        when(campusHeroMapper.selectOne(any())).thenReturn(hero);
        when(postMapper.selectCount(any())).thenReturn(88L);
        when(userMapper.selectList(any())).thenReturn(List.of());

        CampusHeroVO vo = campusHeroService.getByPageKey("custom");

        assertThat(vo.getStatsNumber()).isEqualTo("88+");
        assertThat(vo.getStatsLabel()).isEqualTo("内容已发布");
    }

    @Test
    void delete_delegatesMapper() {
        campusHeroService.delete(7L);
        verify(campusHeroMapper).deleteById(7L);
    }

    private CampusHero hero(Long id, String pageKey, boolean enabled) {
        CampusHero hero = new CampusHero();
        hero.setId(id);
        hero.setPageKey(pageKey);
        hero.setEnabled(enabled);
        hero.setPageName("name");
        hero.setShowStats(true);
        hero.setAvatarUrls(List.of());
        hero.setSortOrder(0);
        return hero;
    }
}
