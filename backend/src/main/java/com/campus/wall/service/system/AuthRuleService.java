package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.AuthRuleDTO;
import com.campus.wall.entity.user.User;
import com.campus.wall.vo.system.AuthRuleVO;

public interface AuthRuleService {

    PageResult<AuthRuleVO> queryRules(int page, int size, String triggerType, String verifyMethod, Boolean enabled);

    Long createRule(AuthRuleDTO dto);

    void updateRule(Long id, AuthRuleDTO dto);

    void deleteRule(Long id);

    void applyRules(User user, String triggerType, String verifyMethod);
}
