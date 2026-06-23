package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getAll() {
        return settingsService.getAllSettings();
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> update(@RequestBody Map<String, String> updates) {
        // userId will be extracted from SecurityContext in a real app;
        // for now passing null (the column is nullable).
        return settingsService.updateSettings(updates, null);
    }
}
