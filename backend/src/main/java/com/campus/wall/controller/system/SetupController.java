package com.campus.wall.controller.system;

import com.campus.wall.common.R;
import com.campus.wall.dto.system.SetupInitDTO;
import com.campus.wall.service.system.SetupService;
import com.campus.wall.vo.system.SetupStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统初始化", description = "部署初始化向导")
@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    @Operation(summary = "初始化状态")
    @GetMapping("/status")
    public R<SetupStatusVO> status() {
        return R.ok(setupService.getStatus());
    }

    @Operation(summary = "执行初始化")
    @PostMapping("/init")
    public R<Void> init(@Valid @RequestBody SetupInitDTO dto) {
        setupService.initialize(dto);
        return R.ok();
    }
}
