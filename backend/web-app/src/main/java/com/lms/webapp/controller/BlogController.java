package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lms.webapp.client.GatewayClient;
import com.lms.webapp.dto.form.CommentForm;
import com.lms.webapp.dto.form.PostForm;
import com.lms.webapp.dto.form.VoteForm;
import com.lms.webapp.service.MarkdownService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class BlogController {

    private final GatewayClient gatewayClient;
    private final MarkdownService markdownService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BlogController(GatewayClient gatewayClient, MarkdownService markdownService) {
        this.gatewayClient = gatewayClient;
        this.markdownService = markdownService;
    }

    /**
     * 1. Danh sách bài viết công khai (Public)
     */
    @GetMapping("/blog")
    public String publicBlogList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        // Lấy danh sách thẻ tag
        List<JsonNode> tagsList = loadTags();
        model.addAttribute("tags", tagsList);
        model.addAttribute("selectedTagId", tagId);
        model.addAttribute("keyword", keyword);

        // Gọi Gateway lấy danh sách bài viết
        StringBuilder uri = new StringBuilder("/blog/posts/public?page=").append(page).append("&size=").append(size);
        if (tagId != null) {
            uri.append("&tagId=").append(tagId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            uri.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }

        try {
            JsonNode res = gatewayClient.get(uri.toString(), JsonNode.class);
            JsonNode dataNode = (res != null && res.has("data")) ? res.get("data") : null;
            enrichPostsWithAuthors(dataNode);
            model.addAttribute("posts", dataNode);
        } catch (Exception e) {
            log.warn("Không thể tải danh sách bài viết từ Gateway: {}", e.getMessage());
            model.addAttribute("posts", null);
            model.addAttribute("errorMessage", "Không thể tải danh sách bài viết vào lúc này.");
        }

        return "blog/index";
    }

    /**
     * Endpoint API công khai lấy danh sách thẻ tags
     */
    @GetMapping("/blog/tags")
    @ResponseBody
    public ResponseEntity<List<JsonNode>> getTagsApi() {
        return ResponseEntity.ok(loadTags());
    }

    /**
     * 2. Chi tiết bài viết qua Slug (Public)
     */
    @GetMapping("/blog/{slug:[a-zA-Z0-9\\-]+}")
    public String publicPostDetailBySlug(@PathVariable String slug, Model model) {
        try {
            JsonNode res = gatewayClient.get("/blog/posts/public/" + slug, JsonNode.class);
            JsonNode post = (res != null && res.has("data")) ? res.get("data") : res;
            if (post == null || post.isNull() || !post.isObject()) {
                return "error/404";
            }

            enrichPostWithAuthor(post);
            model.addAttribute("post", post);

            // Render Markdown an toàn
            String rawContent = post.hasNonNull("content") ? post.get("content").asText() : "";
            String renderedContent = markdownService.renderHtml(rawContent);
            model.addAttribute("renderedContent", renderedContent);

            // Lấy bình luận của bài viết
            Long postId = post.hasNonNull("id") ? post.get("id").asLong() : null;
            if (postId != null) {
                loadCommentsIntoModel(postId, model);
            }

            return "blog/detail";
        } catch (RestClientResponseException e) {
            log.warn("Lỗi khi tải bài viết slug {}: {}", slug, e.getMessage());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return "error/404";
            }
            model.addAttribute("errorMessage", "Lỗi tải bài viết: " + e.getResponseBodyAsString());
            return "error/500";
        } catch (Exception e) {
            log.error("Lỗi ngoại lệ khi xem bài viết slug {}: {}", slug, e.getMessage());
            return "error/500";
        }
    }

    /**
     * 3. Chi tiết bài viết qua ID (Authenticated)
     */
    @GetMapping("/blog/view/{id}")
    @PreAuthorize("isAuthenticated()")
    public String postDetailById(@PathVariable Long id, Model model) {
        try {
            JsonNode res = gatewayClient.get("/blog/posts/" + id, JsonNode.class);
            JsonNode post = (res != null && res.has("data")) ? res.get("data") : res;
            if (post == null || post.isNull() || !post.isObject()) {
                return "error/404";
            }

            enrichPostWithAuthor(post);
            model.addAttribute("post", post);

            String rawContent = post.hasNonNull("content") ? post.get("content").asText() : "";
            String renderedContent = markdownService.renderHtml(rawContent);
            model.addAttribute("renderedContent", renderedContent);

            loadCommentsIntoModel(id, model);

            return "blog/detail";
        } catch (RestClientResponseException e) {
            log.warn("Lỗi khi tải bài viết id {}: {}", id, e.getMessage());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return "error/404";
            }
            return "error/500";
        }
    }

    /**
     * 4. Mở form tạo bài viết mới
     */
    @GetMapping("/blog/new")
    @PreAuthorize("isAuthenticated()")
    public String newPostForm(Model model) {
        model.addAttribute("postForm", new PostForm());
        model.addAttribute("tags", loadTags());
        model.addAttribute("isEdit", false);
        return "blog/post-form";
    }

    /**
     * 5. Submit tạo bài viết mới (Multipart)
     */
    @PostMapping("/blog/new")
    @PreAuthorize("isAuthenticated()")
    public String createPost(
            @ModelAttribute("postForm") @Valid PostForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("tags", loadTags());
            model.addAttribute("isEdit", false);
            return "blog/post-form";
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("title", form.getTitle());
            if (form.getSummary() != null && !form.getSummary().trim().isEmpty()) {
                body.add("summary", form.getSummary().trim());
            }
            body.add("content", form.getContent());
            body.add("status", form.getStatus() != null ? form.getStatus() : "PUBLISHED");

            if (form.getTagIds() != null) {
                for (Long tagId : form.getTagIds()) {
                    body.add("tagIds", tagId);
                }
            }

            if (form.getFile() != null && !form.getFile().isEmpty()) {
                body.add("thumbnail", form.getFile().getResource());
            }

            JsonNode res = gatewayClient.postMultipart("/blog/posts", body, JsonNode.class);
            JsonNode data = (res != null && res.has("data")) ? res.get("data") : res;
            if (data != null && data.hasNonNull("slug")) {
                return "redirect:/blog/" + data.get("slug").asText();
            }
            return "redirect:/blog/my-posts";
        } catch (RestClientResponseException e) {
            log.warn("Lỗi tạo bài viết từ Gateway: {}", e.getMessage());
            model.addAttribute("errorMessage", "Không thể tạo bài viết: " + e.getResponseBodyAsString());
            model.addAttribute("tags", loadTags());
            model.addAttribute("isEdit", false);
            return "blog/post-form";
        }
    }

    /**
     * 6. Mở form chỉnh sửa bài viết
     */
    @GetMapping("/blog/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editPostForm(@PathVariable Long id, Model model) {
        try {
            JsonNode res = gatewayClient.get("/blog/posts/" + id, JsonNode.class);
            JsonNode post = (res != null && res.has("data")) ? res.get("data") : res;
            if (post == null || post.isNull()) {
                return "error/404";
            }

            PostForm form = new PostForm();
            form.setId(id);
            form.setTitle(post.hasNonNull("title") ? post.get("title").asText() : "");
            form.setSummary(post.hasNonNull("summary") ? post.get("summary").asText() : "");
            form.setContent(post.hasNonNull("content") ? post.get("content").asText() : "");
            form.setStatus(post.hasNonNull("status") ? post.get("status").asText() : "PUBLISHED");

            List<Long> currentTagIds = new ArrayList<>();
            if (post.has("tags") && post.get("tags").isArray()) {
                for (JsonNode t : post.get("tags")) {
                    if (t.hasNonNull("id")) {
                        currentTagIds.add(t.get("id").asLong());
                    }
                }
            }
            form.setTagIds(currentTagIds);

            model.addAttribute("postForm", form);
            model.addAttribute("tags", loadTags());
            model.addAttribute("isEdit", true);
            model.addAttribute("currentPost", post);
            return "blog/post-form";
        } catch (Exception e) {
            log.error("Lỗi khi mở form sửa bài viết id {}: {}", id, e.getMessage());
            return "error/500";
        }
    }

    /**
     * 7. Submit cập nhật bài viết (Multipart)
     */
    @PostMapping("/blog/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String updatePost(
            @PathVariable Long id,
            @ModelAttribute("postForm") @Valid PostForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("tags", loadTags());
            model.addAttribute("isEdit", true);
            return "blog/post-form";
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("title", form.getTitle());
            if (form.getSummary() != null && !form.getSummary().trim().isEmpty()) {
                body.add("summary", form.getSummary().trim());
            }
            body.add("content", form.getContent());
            body.add("status", form.getStatus() != null ? form.getStatus() : "PUBLISHED");

            if (form.getTagIds() != null && !form.getTagIds().isEmpty()) {
                for (Long tagId : form.getTagIds()) {
                    body.add("tagIds", tagId);
                }
            } else {
                body.add("clearTags", "true");
            }

            if (form.getFile() != null && !form.getFile().isEmpty()) {
                body.add("thumbnail", form.getFile().getResource());
            }

            JsonNode res = gatewayClient.putMultipart("/blog/posts/" + id, body, JsonNode.class);
            JsonNode data = (res != null && res.has("data")) ? res.get("data") : res;
            if (data != null && data.hasNonNull("slug")) {
                return "redirect:/blog/" + data.get("slug").asText();
            }
            return "redirect:/blog/my-posts";
        } catch (RestClientResponseException e) {
            log.warn("Lỗi sửa bài viết id {}: {}", id, e.getMessage());
            model.addAttribute("errorMessage", "Không thể cập nhật bài viết: " + e.getResponseBodyAsString());
            model.addAttribute("tags", loadTags());
            model.addAttribute("isEdit", true);
            return "blog/post-form";
        }
    }

    /**
     * 8. Xóa bài viết cá nhân
     */
    @PostMapping("/blog/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deletePost(@PathVariable Long id) {
        try {
            gatewayClient.delete("/blog/posts/" + id, JsonNode.class);
        } catch (Exception e) {
            log.warn("Lỗi khi xóa bài viết id {}: {}", id, e.getMessage());
        }
        return "redirect:/blog/my-posts";
    }

    /**
     * 9. Trang quản lý bài viết của tôi
     */
    @GetMapping("/blog/my-posts")
    @PreAuthorize("isAuthenticated()")
    public String myPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        StringBuilder uri = new StringBuilder("/blog/posts/me?page=").append(page).append("&size=").append(size);
        if (status != null && !status.trim().isEmpty()) {
            uri.append("&status=").append(status.trim());
        }

        try {
            JsonNode res = gatewayClient.get(uri.toString(), JsonNode.class);
            JsonNode data = (res != null && res.has("data")) ? res.get("data") : res;
            model.addAttribute("posts", data);
            model.addAttribute("currentStatus", status);
        } catch (Exception e) {
            log.warn("Lỗi tải danh sách bài viết cá nhân: {}", e.getMessage());
            model.addAttribute("posts", null);
        }

        return "blog/my-posts";
    }

    /**
     * 10. Ajax: Thêm bình luận
     */
    @PostMapping("/blog/posts/{postId}/comments")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JsonNode> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CommentForm form) {
        try {
            JsonNode res = gatewayClient.post("/blog/posts/" + postId + "/comments", form, JsonNode.class);
            return ResponseEntity.ok(res);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(safeParse(e.getResponseBodyAsString()));
        }
    }

    /**
     * 11. Ajax: Cập nhật bình luận
     */
    @PutMapping("/blog/posts/{postId}/comments/{commentId}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JsonNode> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentForm form) {
        try {
            JsonNode res = gatewayClient.put("/blog/posts/" + postId + "/comments/" + commentId, form, JsonNode.class);
            return ResponseEntity.ok(res);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(safeParse(e.getResponseBodyAsString()));
        }
    }

    /**
     * 12. Ajax: Xóa bình luận
     */
    @DeleteMapping("/blog/posts/{postId}/comments/{commentId}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JsonNode> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        try {
            JsonNode res = gatewayClient.delete("/blog/posts/" + postId + "/comments/" + commentId, JsonNode.class);
            return ResponseEntity.ok(res);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(safeParse(e.getResponseBodyAsString()));
        }
    }

    /**
     * 13. Ajax: Vote bài viết
     */
    @PostMapping("/blog/posts/{postId}/vote")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JsonNode> votePost(
            @PathVariable Long postId,
            @RequestBody @Valid VoteForm form) {
        try {
            JsonNode res = gatewayClient.post("/blog/posts/" + postId + "/vote", form, JsonNode.class);
            return ResponseEntity.ok(res);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(safeParse(e.getResponseBodyAsString()));
        }
    }

    /**
     * 14. Ajax: Vote bình luận
     */
    @PostMapping("/blog/posts/{postId}/comments/{commentId}/vote")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JsonNode> voteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid VoteForm form) {
        try {
            JsonNode res = gatewayClient.post("/blog/posts/" + postId + "/comments/" + commentId + "/vote", form, JsonNode.class);
            return ResponseEntity.ok(res);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(safeParse(e.getResponseBodyAsString()));
        }
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private List<JsonNode> loadTags() {
        List<JsonNode> tagsList = new ArrayList<>();
        try {
            JsonNode tagsRes = gatewayClient.get("/blog/tags", JsonNode.class);
            if (tagsRes != null && tagsRes.has("data") && tagsRes.get("data").isArray()) {
                for (JsonNode t : tagsRes.get("data")) {
                    tagsList.add(t);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải tags từ Gateway: {}", e.getMessage());
        }
        return tagsList;
    }

    private void loadCommentsIntoModel(Long postId, Model model) {
        try {
            JsonNode commentsRes = gatewayClient.get("/blog/posts/" + postId + "/comments?page=0&size=100", JsonNode.class);
            JsonNode data = (commentsRes != null && commentsRes.has("data")) ? commentsRes.get("data") : null;
            enrichCommentsWithAuthors(data);
            model.addAttribute("comments", data);
        } catch (Exception e) {
            log.warn("Không thể tải comments cho bài viết {}: {}", postId, e.getMessage());
            model.addAttribute("comments", null);
        }
    }

    private JsonNode fetchPublicProfile(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode res = gatewayClient.get("/identity/users/" + userId.trim() + "/profile", JsonNode.class);
            return (res != null && res.has("data") && !res.get("data").isNull()) ? res.get("data") : null;
        } catch (Exception e) {
            log.warn("Không thể tải thông tin profile cho userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private void enrichPostWithAuthor(JsonNode post) {
        if (post == null || !post.isObject()) return;
        ObjectNode obj = (ObjectNode) post;
        String authorId = post.hasNonNull("authorId") ? post.get("authorId").asText() : null;
        if (authorId != null && !authorId.trim().isEmpty()) {
            JsonNode profile = fetchPublicProfile(authorId);
            if (profile != null && profile.hasNonNull("fullname") && !profile.get("fullname").asText().trim().isEmpty()) {
                obj.put("authorName", profile.get("fullname").asText());
                if (profile.hasNonNull("avatarUrl")) {
                    obj.put("authorAvatar", profile.get("avatarUrl").asText());
                }
                return;
            }
        }
        if (!obj.hasNonNull("authorName")) {
            obj.put("authorName", "Người dùng LMS");
        }
    }

    private void enrichPostsWithAuthors(JsonNode postsPage) {
        if (postsPage == null || !postsPage.has("content") || !postsPage.get("content").isArray()) return;
        Map<String, JsonNode> cache = new HashMap<>();
        for (JsonNode p : postsPage.get("content")) {
            if (p.isObject()) {
                ObjectNode obj = (ObjectNode) p;
                String authorId = p.hasNonNull("authorId") ? p.get("authorId").asText() : null;
                if (authorId != null && !authorId.trim().isEmpty()) {
                    JsonNode profile = cache.computeIfAbsent(authorId, this::fetchPublicProfile);
                    if (profile != null && profile.hasNonNull("fullname") && !profile.get("fullname").asText().trim().isEmpty()) {
                        obj.put("authorName", profile.get("fullname").asText());
                        if (profile.hasNonNull("avatarUrl")) {
                            obj.put("authorAvatar", profile.get("avatarUrl").asText());
                        }
                        continue;
                    }
                }
                if (!obj.hasNonNull("authorName")) {
                    obj.put("authorName", "Người dùng LMS");
                }
            }
        }
    }

    private void enrichCommentsWithAuthors(JsonNode commentsData) {
        if (commentsData == null || !commentsData.has("content") || !commentsData.get("content").isArray()) return;
        Map<String, JsonNode> cache = new HashMap<>();
        for (JsonNode c : commentsData.get("content")) {
            enrichCommentNodeWithAuthor(c, cache);
        }
    }

    private void enrichCommentNodeWithAuthor(JsonNode commentNode, Map<String, JsonNode> cache) {
        if (commentNode == null || !commentNode.isObject()) return;
        ObjectNode obj = (ObjectNode) commentNode;
        String userId = commentNode.hasNonNull("userId") ? commentNode.get("userId").asText() : null;
        if (userId != null && !userId.trim().isEmpty()) {
            JsonNode profile = cache.computeIfAbsent(userId, this::fetchPublicProfile);
            if (profile != null && profile.hasNonNull("fullname") && !profile.get("fullname").asText().trim().isEmpty()) {
                obj.put("authorName", profile.get("fullname").asText());
                if (profile.hasNonNull("avatarUrl")) {
                    obj.put("authorAvatar", profile.get("avatarUrl").asText());
                }
            }
        }
        if (!obj.hasNonNull("authorName")) {
            obj.put("authorName", "Người dùng");
        }

        if (commentNode.has("replies") && commentNode.get("replies").isArray()) {
            for (JsonNode reply : commentNode.get("replies")) {
                enrichCommentNodeWithAuthor(reply, cache);
            }
        }
    }

    private JsonNode safeParse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
