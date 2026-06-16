package com.campus.wall.vo.campus;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CampusHeroVO {
    private Long id;
    private String pageKey;
    private String pageName;
    private Boolean enabled;
    private String theme;
    private String titleStart;
    private String titleHighlight;
    private String description;
    private String badge;
    private String primaryBtnText;
    private String primaryBtnLink;
    private String secondaryBtnText;
    private String secondaryBtnLink;
    private Boolean showStats;
    private String statsNumber;
    private String statsLabel;
    private List<String> avatarUrls;
    private List<String> avatarNames;
    private String floatCardLabel;
    private String floatCardValue;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
