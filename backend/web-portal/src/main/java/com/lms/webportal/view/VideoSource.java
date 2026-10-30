package com.lms.webportal.view;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/** Validated presentation data; no remote requests are made while resolving a video. */
public record VideoSource(String url, boolean embed) {
    private static final VideoSource UNAVAILABLE = new VideoSource("", false);
    private static final Set<String> YOUTUBE_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com");
    private static final Set<String> PRIVATE_YOUTUBE_HOSTS = Set.of("youtube-nocookie.com", "www.youtube-nocookie.com");

    public static VideoSource from(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.indexOf('\\') >= 0
                || rawUrl.chars().anyMatch(c -> c < 32 || c == 127)) {
            return UNAVAILABLE;
        }
        String url = rawUrl.trim();
        try {
            URI uri = new URI(url);
            if (url.startsWith("/") && !url.startsWith("//") && uri.getRawAuthority() == null) {
                return new VideoSource(url, false);
            }
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                return UNAVAILABLE;
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath();
            if (YOUTUBE_HOSTS.contains(host) || PRIVATE_YOUTUBE_HOSTS.contains(host) || host.equals("youtu.be")) {
                String id = null;
                if (host.equals("youtu.be")) {
                    id = path.substring(1);
                } else if (path.startsWith("/embed/")) {
                    id = path.substring("/embed/".length());
                } else if (YOUTUBE_HOSTS.contains(host) && path.equals("/watch") && uri.getRawQuery() != null) {
                    for (String pair : uri.getRawQuery().split("&")) {
                        if (pair.startsWith("v=")) {
                            id = URLDecoder.decode(pair.substring(2), StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                if (id == null || !id.matches("[A-Za-z0-9_-]+")) return UNAVAILABLE;
                String embedHost = PRIVATE_YOUTUBE_HOSTS.contains(host) ? "www.youtube-nocookie.com" : "www.youtube.com";
                return new VideoSource("https://" + embedHost + "/embed/" + id, true);
            }
            if (Set.of("vimeo.com", "www.vimeo.com", "player.vimeo.com").contains(host)) {
                String id = host.equals("player.vimeo.com") && path.startsWith("/video/")
                        ? path.substring("/video/".length()) : path.substring(1);
                if (!id.matches("[0-9]+")) return UNAVAILABLE;
                return new VideoSource("https://player.vimeo.com/video/" + id, true);
            }
            // Other HTTP(S) sources are media only, never executable iframe documents.
            return new VideoSource(url, false);
        } catch (URISyntaxException | IllegalArgumentException | IndexOutOfBoundsException exception) {
            return UNAVAILABLE;
        }
    }

    public static VideoSource fromContent(JsonNode content) {
        if (content == null || !content.isObject()) return UNAVAILABLE;
        for (String key : new String[]{"videoUrl", "video_url", "url", "embedUrl"}) {
            if (content.hasNonNull(key) && !content.get(key).asText().isBlank()) {
                return from(content.get(key).asText());
            }
        }
        return UNAVAILABLE;
    }
}
