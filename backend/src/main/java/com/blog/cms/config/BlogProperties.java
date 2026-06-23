package com.blog.cms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Type-safe configuration for blog.* properties (auth, uploads, etc.)
 */
@Component
@ConfigurationProperties(prefix = "blog")
@Getter
@Setter
public class BlogProperties {

    private Site site = new Site();
    private Upload upload = new Upload();
    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private Csrf csrf = new Csrf();

    @Getter
    @Setter
    public static class Site {
        private String title = "My Blog";
        private String description = "A personal blog";
        private String url = "http://localhost:8080";
    }

    @Getter
    @Setter
    public static class Upload {
        private String baseDir = "/data/uploads";
        private long maxFileSizeBytes = 10_485_760L;
        private List<String> allowedMimeTypes = List.of(
                "image/jpeg", "image/png", "image/webp", "image/gif"
        );
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenTtlSeconds = 900;
        private long refreshTokenTtlSeconds = 604_800L;
    }

    @Getter
    @Setter
    public static class Cookie {
        private String name = "blog_session";
        private boolean secure = false;
        private boolean httpOnly = true;
        private String sameSite = "Lax";
        private String domain = "";
    }

    @Getter
    @Setter
    public static class Csrf {
        private boolean enabled = true;
        private String headerName = "X-CSRF-Token";
    }
}
