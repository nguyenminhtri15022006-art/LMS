package com.lms.webapp.service;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class MarkdownService {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "mailto", "tel");

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

        // Loại bỏ thẻ script và iframe thô trước khi parse
        String preSanitized = markdown
                .replaceAll("(?is)<script.*?>.*?</script>", "")
                .replaceAll("(?is)<iframe.*?>.*?</iframe>", "");

        Node document = parser.parse(preSanitized);

        // Duyệt AST để kiểm soát toàn bộ Link và Image URL sau giải mã
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Link link) {
                link.setDestination(sanitizeDestination(link.getDestination()));
                visitChildren(link);
            }

            @Override
            public void visit(Image image) {
                image.setDestination(sanitizeDestination(image.getDestination()));
                visitChildren(image);
            }
        });

        String html = renderer.render(document);

        // Bảo vệ bổ sung lần 2 trên HTML kết quả
        return html.replaceAll("(?is)<script.*?>.*?</script>", "")
                   .replaceAll("(?is)&lt;script.*?&gt;.*?&lt;/script&gt;", "")
                   .replaceAll("(?is)<iframe.*?>.*?</iframe>", "")
                   .replaceAll("(?is)&lt;iframe.*?&gt;.*?&lt;/iframe&gt;", "");
    }

    private String sanitizeDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            return "#";
        }

        // Giải mã HTML entities (ví dụ: javascript&#58; -> javascript:)
        String decoded = HtmlUtils.htmlUnescape(destination.trim());

        // Thử giải mã URL-encoded (ví dụ: java%73cript: -> javascript:)
        try {
            decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }

        // Loại bỏ các ký tự whitespace và control characters ẩn
        String cleanProtocol = decoded.replaceAll("[\\s\\u0000-\\u001F\\u007F-\\u009F]", "").toLowerCase();

        // Kiểm tra protocol nếu có dấu hai chấm ':'
        int colonIdx = cleanProtocol.indexOf(':');
        if (colonIdx != -1) {
            String scheme = cleanProtocol.substring(0, colonIdx);
            if (!ALLOWED_SCHEMES.contains(scheme)) {
                return "#";
            }
        }

        return destination;
    }
}
