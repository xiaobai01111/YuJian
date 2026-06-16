package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.campus.CampusHeroDTO;
import com.campus.wall.vo.campus.CampusHeroVO;

public interface CampusHeroService {

    PageResult<CampusHeroVO> query(int page, int size, String keyword, Boolean enabled);

    CampusHeroVO getById(Long id);

    CampusHeroVO create(CampusHeroDTO dto);

    CampusHeroVO update(Long id, CampusHeroDTO dto);

    void delete(Long id);

    CampusHeroVO getByPageKey(String pageKey);
}
