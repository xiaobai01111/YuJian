package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.OnlineUserQueryDTO;
import com.campus.wall.vo.system.OnlineUserVO;

public interface OnlineUserService {

    PageResult<OnlineUserVO> queryOnlineUsers(OnlineUserQueryDTO query);

    void kickoutByToken(String token);
}
