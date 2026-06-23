package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.TagRequest;
import com.blog.cms.content.dto.TagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<List<TagResponse>> list() {
        return tagService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<TagResponse> getById(@PathVariable Long id) {
        return tagService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TagResponse> create(@Valid @RequestBody TagRequest req) {
        return tagService.create(req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return tagService.delete(id);
    }
}
