package com.campus.wall.controller.campus;

import cn.dev33.satoken.annotation.SaIgnore;
import com.campus.wall.common.R;
import com.campus.wall.service.system.CampusHeroService;
import com.campus.wall.vo.campus.CampusHeroVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/campus/heroes")
public class CampusHeroController {

    private final CampusHeroService campusHeroService;

    @SaIgnore
    @GetMapping("/{pageKey}")
    public R<CampusHeroVO> getByPageKey(@PathVariable String pageKey) {
        return R.ok(campusHeroService.getByPageKey(pageKey));
    }
}
