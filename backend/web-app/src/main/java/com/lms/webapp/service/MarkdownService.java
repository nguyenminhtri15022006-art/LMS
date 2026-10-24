package com.lms.webapp.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder()
                .escapeHtml(true)
                .build();
    }

    public String renderHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        // Loại bỏ triệt để các thẻ script, iframe nguy hiểm và nội dung mã độc trước khi render
        String sanitized = markdown
                .replaceAll("(?is)<script.*?>.*?</script>", "")
                .replaceAll("(?is)<iframe.*?>.*?</iframe>", "")
                .replaceAll("(?i)javascript:", "");

        Node document = parser.parse(sanitized);
        String html = renderer.render(document);

        // Bảo vệ lần 2 trên HTML kết quả
        return html.replaceAll("(?is)<script.*?>.*?</script>", "")
                   .replaceAll("(?is)&lt;script.*?&gt;.*?&lt;/script&gt;", "")
                   .replaceAll("(?is)<iframe.*?>.*?</iframe>", "")
                   .replaceAll("(?is)&lt;iframe.*?&gt;.*?&lt;/iframe&gt;", "");
    }
}
