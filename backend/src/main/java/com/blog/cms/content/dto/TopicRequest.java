package com.blog.cms.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TopicRequest {

    @NotBlank @Size(max = 100)
    private String name;

    @Size(max = 120)
    private String slug; // auto-generated if blank

    @Size(max = 500)
    private String description;

    @Size(max = 7)
    private String color;

    @Size(max = 50)
    private String icon;

    private Long parentId;

    private Integer sortOrder;
}
