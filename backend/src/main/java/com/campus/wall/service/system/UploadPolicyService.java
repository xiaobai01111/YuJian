package com.campus.wall.service.system;

import com.campus.wall.dto.system.UploadPolicyUpdateDTO;
import com.campus.wall.entity.system.SysUploadPolicy;
import com.campus.wall.vo.system.UploadPolicyVO;

import java.util.List;

public interface UploadPolicyService {
    List<UploadPolicyVO> listPolicies();

    UploadPolicyVO updatePolicy(String sceneCode, UploadPolicyUpdateDTO dto);

    SysUploadPolicy findByScene(String sceneCode);
}
