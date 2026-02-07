package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.UploadPolicyUpdateDTO;
import com.campus.wall.service.system.UploadPolicyService;
import com.campus.wall.vo.system.UploadPolicyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/console/upload-policies")
@RequiredArgsConstructor
@Validated
public class UploadPolicyController {

    private final UploadPolicyService uploadPolicyService;

    @GetMapping
    @SaCheckPermission("system:upload-policy:list")
    public R<List<UploadPolicyVO>> listPolicies() {
        return R.ok(uploadPolicyService.listPolicies());
    }

    @PutMapping("/{sceneCode}")
    @SaCheckPermission("system:upload-policy:edit")
    public R<UploadPolicyVO> updatePolicy(@PathVariable String sceneCode,
                                          @RequestBody @Valid UploadPolicyUpdateDTO dto) {
        return R.ok(uploadPolicyService.updatePolicy(sceneCode, dto));
    }
}
