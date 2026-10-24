package com.lms.webapp.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoSourceTest {
    @Test
    void blocksExecutableAndEncodedSchemes() {
        for (String url : new String[]{"javascript:alert(1)//youtube.com", "data:text/html,<script></script>",
                "javascript&#58;alert(1)//youtube.com", "java%73cript:alert(1)", "//youtube.com/embed/a"}) {
            assertThat(VideoSource.from(url).url()).as(url).isEmpty();
        }
    }

    @Test
    void rejectsMalformedCredentialsAndControlCharacters() {
        for (String url : new String[]{"https://youtube.com@evil.example/embed/a", "https://evil.example\\@youtube.com/embed/a",
                "https://youtube.com/\nembed/a", "https://", "   "}) {
            assertThat(VideoSource.from(url).url()).as(url).isEmpty();
        }
    }

    @Test
    void onlyTrustedHostsCanEmbed() {
        for (String url : new String[]{"https://youtube.com.evil.example/embed/a", "https://evil.example/youtube.com",
                "https://evil.example/embed/a", "https://evil.example/?next=https://vimeo.com/123"}) {
            assertThat(VideoSource.from(url).embed()).as(url).isFalse();
        }
    }

    @Test
    void acceptsDirectHttpAndRootRelativeMedia() {
        for (String url : new String[]{"http://localhost:9000/videos/a.mp4", "https://cdn.example/a.webm", "/media/a.mp4"}) {
            assertThat(VideoSource.from(url).url()).isEqualTo(url);
            assertThat(VideoSource.from(url).embed()).isFalse();
        }
    }

    @Test
    void preservesSignedDirectMediaUrl() {
        String url = "https://storage.example/a.mp4?X-Amz-Signature=abc%2B123&X-Amz-Expires=600";
        assertThat(VideoSource.from(url).url()).isEqualTo(url);
    }

    @Test
    void normalizesYoutubeLinksAndRetainsValidEmbed() {
        for (String url : new String[]{"https://youtu.be/abc_123", "https://www.youtube.com/watch?v=abc_123",
                "https://www.youtube.com/embed/abc_123"}) {
            assertThat(VideoSource.from(url).url()).isEqualTo("https://www.youtube.com/embed/abc_123");
            assertThat(VideoSource.from(url).embed()).isTrue();
        }
        assertThat(VideoSource.from("https://www.youtube-nocookie.com/embed/abc_123").embed()).isTrue();
    }

    @Test
    void normalizesVimeoLinksAndRejectsNonVideoProviderPaths() {
        assertThat(VideoSource.from("https://vimeo.com/12345").url()).isEqualTo("https://player.vimeo.com/video/12345");
        assertThat(VideoSource.from("https://player.vimeo.com/video/12345").embed()).isTrue();
        assertThat(VideoSource.from("https://www.youtube.com/login").url()).isEmpty();
    }

    @Test
    void readsSupportedContentAliasesAndHandlesMissingContent() throws Exception {
        var mapper = new ObjectMapper();
        for (String key : new String[]{"videoUrl", "video_url", "url", "embedUrl"}) {
            var content = mapper.createObjectNode().put(key, "https://cdn.example/a.mp4");
            assertThat(VideoSource.fromContent(content).url()).isEqualTo("https://cdn.example/a.mp4");
        }
        assertThat(VideoSource.fromContent(null).url()).isEmpty();
        assertThat(VideoSource.fromContent(mapper.nullNode()).url()).isEmpty();
    }
}
