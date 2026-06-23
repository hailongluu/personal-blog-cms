package com.blog.cms.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TagRequest {

    @NotBlank @Size(max = 50)
    private String name;

    @Size(max = 60)
    private String slug; // auto-generated if blank
}
