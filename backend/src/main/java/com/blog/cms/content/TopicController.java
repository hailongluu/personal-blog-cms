package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import com.blog.cms.content.dto.TopicRequest;
import com.blog.cms.content.dto.TopicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<List<TopicResponse>> list() {
        return topicService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','AUTHOR')")
    public ApiResponse<TopicResponse> getById(@PathVariable Long id) {
        return topicService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TopicResponse> create(@Valid @RequestBody TopicRequest req) {
        return topicService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TopicResponse> update(@PathVariable Long id, @Valid @RequestBody TopicRequest req) {
        return topicService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return topicService.delete(id);
    }
}
