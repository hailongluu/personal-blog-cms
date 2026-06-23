package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.MediaResponse;
import com.blog.cms.security.BlogUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<List<MediaResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return mediaService.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<MediaResponse> getById(@PathVariable Long id) {
        return mediaService.findById(id);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MediaResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal BlogUserDetails user) {
        return mediaService.upload(file, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return mediaService.delete(id);
    }
}
