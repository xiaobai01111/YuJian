package com.campus.wall.service.system;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.entity.system.SysMenu;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.service.system.impl.MenuServiceImpl;
import com.campus.wall.vo.system.MenuVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    void createMenu_invalidTopLevelPath_throws() {
        MenuVO menu = new MenuVO();
        menu.setParentId(0L);
        menu.setPath("/user/home");

        assertThatThrownBy(() -> menuService.createMenu(menu))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("控制台顶级菜单路径必须以 /console 开头");
    }

    @Test
    void updateMenu_missingMenu_throws() {
        MenuVO menu = new MenuVO();
        menu.setParentId(0L);
        menu.setPath("/console/system/user");

        when(sysMenuMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> menuService.updateMenu(10L, menu))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode())
            .hasMessage("菜单不存在");
    }

    @Test
    void deleteMenu_hasChildren_throws() {
        when(sysMenuMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> menuService.deleteMenu(5L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("存在子菜单，无法删除");
    }

    @Test
    void createMenu_validPath_inserts() {
        MenuVO menu = new MenuVO();
        menu.setParentId(0L);
        menu.setPath("/console/system/user");

        menuService.createMenu(menu);

        verify(sysMenuMapper).insert(any(SysMenu.class));
    }
}
