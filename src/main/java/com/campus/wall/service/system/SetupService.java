package com.campus.wall.service.system;

import com.campus.wall.dto.system.SetupInitDTO;
import com.campus.wall.vo.system.SetupStatusVO;

public interface SetupService {

    SetupStatusVO getStatus();

    void initialize(SetupInitDTO dto);
}
