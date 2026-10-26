package com.lms.webapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownServiceTest {

    private MarkdownService markdownService;

    @BeforeEach
    void setUp() {
        markdownService = new MarkdownService();
    }

    @Test
    @DisplayName("Tái hiện lỗ hổng XSS: href chứa javascript entity mã hóa như javascript&#58; hoặc java&#x73;cript: phải bị trung hòa thành #")
    void renderHtml_WithHtmlEntityEncodedJavascriptProtocol_NeutralizesToHash() {
        // Tái hiện chính xác 2 trường hợp người dùng nêu
        String input1 = "[click](javascript&#58;alert%281%29)";
        String input2 = "[click](java&#x73;cript:alert%281%29)";

        String html1 = markdownService.renderHtml(input1);
        String html2 = markdownService.renderHtml(input2);

        // Với code cũ (chỉ regex), html1 sẽ chứa href="javascript&#58;alert%281%29" hoặc javascript:
        // Assertion yêu cầu: href không được chứa giao thức nguy hiểm sau giải mã, phải bị trung hòa thành #
        assertThat(html1).doesNotContain("javascript&#58;");
        assertThat(html1).doesNotContain("javascript:");
        assertThat(html1).contains("href=\"#\"");

        assertThat(html2).doesNotContain("java&#x73;cript:");
        assertThat(html2).doesNotContain("javascript:");
        assertThat(html2).contains("href=\"#\"");
    }

    @Test
    @DisplayName("Chặn các giao thức nguy hiểm khác như vbscript: và data:text/html")
    void renderHtml_WithVbscriptAndDataProtocol_NeutralizesToHash() {
        String inputVb = "[test](vbscript:msgbox(1))";
        String inputData = "[test](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)";

        String htmlVb = markdownService.renderHtml(inputVb);
        String htmlData = markdownService.renderHtml(inputData);

        assertThat(htmlVb).doesNotContain("vbscript:");
        assertThat(htmlVb).contains("href=\"#\"");

        assertThat(htmlData).doesNotContain("data:");
        assertThat(htmlData).contains("href=\"#\"");
    }

    @Test
    @DisplayName("Giữ nguyên các liên kết an toàn: http, https, mailto, tel và relative URL")
    void renderHtml_WithSafeUrls_PreservesUrls() {
        String input = "[Trang chủ](https://example.com/courses?id=1#overview) và [Liên hệ](mailto:support@example.com) và [Nội bộ](/docs/readme)";

        String html = markdownService.renderHtml(input);

        assertThat(html).contains("href=\"https://example.com/courses?id=1#overview\"");
        assertThat(html).contains("href=\"mailto:support@example.com\"");
        assertThat(html).contains("href=\"/docs/readme\"");
    }
}
