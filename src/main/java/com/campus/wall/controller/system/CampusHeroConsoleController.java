package com.campus.wall.controller.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.campus.CampusHeroDTO;
import com.campus.wall.service.system.CampusHeroService;
import com.campus.wall.vo.campus.CampusHeroVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/console/campus/heroes")
public class CampusHeroConsoleController {

    private final CampusHeroService campusHeroService;

    @GetMapping
    public R<PageResult<CampusHeroVO>> query(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return R.ok(campusHeroService.query(page, size, keyword, enabled));
    }

    @GetMapping("/{id}")
    public R<CampusHeroVO> detail(@PathVariable Long id) {
        return R.ok(campusHeroService.getById(id));
    }

    @PostMapping
    public R<CampusHeroVO> create(@RequestBody CampusHeroDTO dto) {
        return R.ok(campusHeroService.create(dto));
    }

    @PutMapping("/{id}")
    public R<CampusHeroVO> update(@PathVariable Long id, @RequestBody CampusHeroDTO dto) {
        return R.ok(campusHeroService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        campusHeroService.delete(id);
        return R.ok();
    }
}
