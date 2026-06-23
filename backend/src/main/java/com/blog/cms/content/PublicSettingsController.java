package com.blog.cms.content;

import com.blog.cms.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/settings")
@RequiredArgsConstructor
public class PublicSettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getPublic() {
        return settingsService.getPublicSettings();
    }
}
