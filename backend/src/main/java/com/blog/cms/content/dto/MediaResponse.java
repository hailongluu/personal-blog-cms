package com.blog.cms.content.dto;

import com.blog.cms.content.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder @AllArgsConstructor
public class MediaResponse {
    private Long id;
    private String filename;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
    private Integer width;
    private Integer height;
    private String publicUrl;
    private String altText;
    private String caption;
    private Instant createdAt;

    public static MediaResponse from(Media m) {
        return MediaResponse.builder()
            .id(m.getId())
            .filename(m.getFilename())
            .originalName(m.getOriginalName())
            .mimeType(m.getMimeType())
            .sizeBytes(m.getSizeBytes())
            .width(m.getWidth())
            .height(m.getHeight())
            .publicUrl(m.getPublicUrl())
            .altText(m.getAltText())
            .caption(m.getCaption())
            .createdAt(m.getCreatedAt())
            .build();
    }
}
