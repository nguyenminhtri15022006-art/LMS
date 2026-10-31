package com.lms.webportal.service;



import com.fasterxml.jackson.databind.JsonNode;

import com.lms.webportal.client.GatewayClient;

import com.lms.webportal.dto.ChapterDto;

import com.lms.webportal.dto.CourseDto;

import com.lms.webportal.dto.CourseFilterDto;

import com.lms.webportal.dto.LessonDto;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;



import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.stream.Collectors;



@Service

@Slf4j

public class CourseService {



    private static final Map<String, String> LESSON_VIDEOS = Map.ofEntries(

        // Course 1 (ThS. Nguyễn Minh Trí - Spring Boot 3)

        Map.entry("12000000-0000-0000-0000-000000000001", "https://www.youtube.com/watch?v=VtHzmiqfld8"),

        Map.entry("12000000-0000-0000-0000-000000000002", "https://www.youtube.com/watch?v=vtPkZShrvXQ"),

        Map.entry("12000000-0000-0000-0000-000000000003", "https://www.youtube.com/watch?v=1b92Q5fE1nI"),

        Map.entry("12000000-0000-0000-0000-000000000004", "https://www.youtube.com/watch?v=n-WzWd5Pz9w"),

        Map.entry("12000000-0000-0000-0000-000000000005", "https://www.youtube.com/watch?v=8SGI_XS5OPw"),

        Map.entry("12000000-0000-0000-0000-000000000006", "https://www.youtube.com/watch?v=5r3qu8tVfQ0"),



        // Course 2 (ThS. Nguyễn Minh Trí - Microservices)

        Map.entry("22000000-0000-0000-0000-000000000001", "https://www.youtube.com/watch?v=GN6ICac3OXY"),

        Map.entry("22000000-0000-0000-0000-000000000002", "https://www.youtube.com/watch?v=GhybmzE7dGQ"),

        Map.entry("22000000-0000-0000-0000-000000000003", "https://www.youtube.com/watch?v=BVdQ3An_h34"),

        Map.entry("22000000-0000-0000-0000-000000000004", "https://www.youtube.com/watch?v=9hP13gJ28aY"),

        Map.entry("22000000-0000-0000-0000-000000000005", "https://www.youtube.com/watch?v=fqMOX6JJhGo"),

        Map.entry("22000000-0000-0000-0000-000000000006", "https://www.youtube.com/watch?v=17X2x0w0jYg"),



        // Course 3 (ThS. Trần Quốc Bảo - Cloud Native, K8s & DevOps)

        Map.entry("32000000-0000-0000-0000-000000000001", "https://www.youtube.com/watch?v=3c-iBn73dDE"),

        Map.entry("32000000-0000-0000-0000-000000000002", "https://www.youtube.com/watch?v=fqMOX6JJhGo"),

        Map.entry("32000000-0000-0000-0000-000000000003", "https://www.youtube.com/watch?v=X48VuDVv0do"),

        Map.entry("32000000-0000-0000-0000-000000000004", "https://www.youtube.com/watch?v=rP1ZcM_xHvg"),

        Map.entry("32000000-0000-0000-0000-000000000005", "https://www.youtube.com/watch?v=R8_veQiYErr"),

        Map.entry("32000000-0000-0000-0000-000000000006", "https://www.youtube.com/watch?v=40608X92eAE"),



        // Course 4 (TS. Lê Thị Mai Linh - React 18 & Next.js)

        Map.entry("42000000-0000-0000-0000-000000000001", "https://www.youtube.com/watch?v=SqcY0GlETPk"),

        Map.entry("42000000-0000-0000-0000-000000000002", "https://www.youtube.com/watch?v=THL1OPboV00"),

        Map.entry("42000000-0000-0000-0000-000000000003", "https://www.youtube.com/watch?v=wm5gMKuwSYk"),

        Map.entry("42000000-0000-0000-0000-000000000004", "https://www.youtube.com/watch?v=vwSlYG7hFk0"),



        // Course 5 (TS. Lê Thị Mai Linh - GenAI & Spring AI)

        Map.entry("52000000-0000-0000-0000-000000000001", "https://www.youtube.com/watch?v=sW7l060iRau"),

        Map.entry("52000000-0000-0000-0000-000000000002", "https://www.youtube.com/watch?v=zbjXXmWxXps"),

        Map.entry("52000000-0000-0000-0000-000000000003", "https://www.youtube.com/watch?v=2TJxpyO3ei4"),

        Map.entry("52000000-0000-0000-0000-000000000004", "https://www.youtube.com/watch?v=h02ft4qUjHQ")

    );



    private final GatewayClient gatewayClient;



    public CourseService(GatewayClient gatewayClient) {

        this.gatewayClient = gatewayClient;

    }



    public Long resolveCategoryId(String catNameOrSlug) {
        if (catNameOrSlug == null || catNameOrSlug.isBlank() || "ALL".equalsIgnoreCase(catNameOrSlug)) {
            return null;
        }
        try {
            return Long.parseLong(catNameOrSlug.trim());
        } catch (NumberFormatException ignored) {}

        try {
            JsonNode res = gatewayClient.get("/course/categories", JsonNode.class);
            if (res != null && res.has("data") && res.get("data").isArray()) {
                for (JsonNode c : res.get("data")) {
                    String name = c.path("name").asText();
                    String slug = c.path("slug").asText();
                    if (catNameOrSlug.equalsIgnoreCase(name) || catNameOrSlug.equalsIgnoreCase(slug)) {
                        return c.path("id").asLong();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể phân giải categoryId từ {}: {}", catNameOrSlug, e.getMessage());
        }
        if ("Công nghệ Thông tin".equalsIgnoreCase(catNameOrSlug) || "cong-nghe-thong-tin".equalsIgnoreCase(catNameOrSlug)) return 9001L;
        if ("Lập trình Web".equalsIgnoreCase(catNameOrSlug) || "lap-trinh-web".equalsIgnoreCase(catNameOrSlug)) return 9002L;
        if ("Kiến trúc Microservices".equalsIgnoreCase(catNameOrSlug) || "kien-truc-microservices".equalsIgnoreCase(catNameOrSlug)) return 9003L;
        if ("Cơ sở Dữ liệu & Cloud".equalsIgnoreCase(catNameOrSlug) || "co-so-du-lieu-va-cloud".equalsIgnoreCase(catNameOrSlug)) return 9004L;
        return null;
    }

    public List<CourseDto> getCourses(CourseFilterDto filter) {
        try {
            StringBuilder uri = new StringBuilder("/course/courses?page=0&size=50");
            Long categoryId = null;
            if (filter != null) {
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    uri.append("&keyword=").append(URLEncoder.encode(filter.getKeyword().trim(), StandardCharsets.UTF_8));
                }
                if (filter.getCategory() != null && !filter.getCategory().isBlank() && !"ALL".equalsIgnoreCase(filter.getCategory())) {
                    categoryId = resolveCategoryId(filter.getCategory());
                    if (categoryId != null) {
                        uri.append("&categoryId=").append(categoryId);
                    }
                }
                if (filter.getDifficulty() != null && !filter.getDifficulty().isBlank() && !"ALL".equalsIgnoreCase(filter.getDifficulty())) {
                    uri.append("&level=").append(URLEncoder.encode(filter.getDifficulty().trim(), StandardCharsets.UTF_8));
                }
            }

            JsonNode response = gatewayClient.get(uri.toString(), JsonNode.class);
            if (response != null && response.has("data")) {
                JsonNode data = response.get("data");
                JsonNode content = data.has("content") ? data.get("content") : data;
                if (content.isArray()) {
                    List<CourseDto> list = new ArrayList<>();
                    for (JsonNode item : content) {
                        list.add(mapJsonToCourseDto(item));
                    }

                    // Bộ lọc bổ trợ chính xác 100% theo tên danh mục và tags
                    if (filter != null && filter.getCategory() != null && !filter.getCategory().isBlank() && !"ALL".equalsIgnoreCase(filter.getCategory())) {
                        final String selectedCat = filter.getCategory().trim();
                        list = list.stream().filter(c -> {
                            if (selectedCat.equalsIgnoreCase(c.getCategory())) return true;
                            if (c.getTags() != null) {
                                for (String tag : c.getTags()) {
                                    if (selectedCat.equalsIgnoreCase(tag)) return true;
                                }
                            }
                            return false;
                        }).collect(Collectors.toList());
                    }

                    return list;
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách khóa học từ Gateway: {}", e.getMessage());
        }
        return Collections.emptyList();
    }



    public CourseDto getCourseById(String id) {

        if (id == null || id.isBlank()) return null;

        try {

            JsonNode response = gatewayClient.get("/course/courses/" + id, JsonNode.class);

            if (response != null && response.has("data")) {

                CourseDto course = mapJsonToCourseDto(response.get("data"));



                // Tải cấu trúc chương mục (Chapters & Lessons) từ MongoDB

                try {

                    JsonNode structureRes = gatewayClient.get("/course/courses/" + id + "/structure", JsonNode.class);

                    if (structureRes != null && structureRes.has("data")) {

                        JsonNode structData = structureRes.get("data");

                        if (structData.has("nodes") && structData.get("nodes").isArray()) {

                            List<ChapterDto> chapters = mapStructureNodesToChapters(id, structData.get("nodes"));

                            course.setChapters(chapters);

                            int total = 0;

                            for (ChapterDto ch : chapters) {

                                if (ch.getLessons() != null) total += ch.getLessons().size();

                            }

                            if (total > 0) {

                                course.setTotalLessons(total);

                            }

                        }

                    }

                } catch (Exception se) {

                    log.warn("Không thể tải cấu trúc khóa học {}: {}", id, se.getMessage());

                }



                return course;

            }

        } catch (Exception e) {

            log.error("Lỗi khi lấy thông tin khóa học {}: {}", id, e.getMessage());

        }

        return null;

    }



    public LessonDto getLessonById(String courseId, String lessonId) {

        CourseDto course = getCourseById(courseId);

        if (course == null || course.getChapters() == null) {

            return null;

        }



        LessonDto target = null;

        for (ChapterDto chapter : course.getChapters()) {

            if (chapter.getLessons() != null) {

                for (LessonDto lesson : chapter.getLessons()) {

                    if (lesson.getId().equals(lessonId)) {

                        target = lesson;

                        break;

                    }

                }

            }

            if (target != null) break;

        }



        if (target != null) {

            try {

                JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/lessons/" + lessonId, JsonNode.class);

                if (res != null && res.has("data")) {

                    JsonNode data = res.get("data");

                    if (data.has("content") && data.get("content").isObject()) {

                        JsonNode content = data.get("content");

                        if (content.hasNonNull("videoUrl")) {

                            target.setVideoUrl(formatYoutubeUrl(content.get("videoUrl").asText()));

                        }

                        if (content.hasNonNull("description")) {

                            target.setContent(content.get("description").asText());

                        }

                    }

                }

            } catch (Exception e) {

                log.warn("Không thể tải chi tiết bài học {} từ course-service: {}", lessonId, e.getMessage());

            }

        }

        return target;

    }



    public boolean isUserEnrolled(String courseId) {

        if (courseId == null || courseId.isBlank()) return false;

        try {

            JsonNode res = gatewayClient.get("/course/courses/enrolled", JsonNode.class);

            if (res != null && res.has("data") && res.get("data").isArray()) {

                for (JsonNode item : res.get("data")) {

                    if (item.hasNonNull("id") && courseId.equals(item.get("id").asText())) {

                        return true;

                    }

                }

            }

        } catch (Exception ignored) {}

        return false;

    }



    public boolean enroll(String courseId, String userId) {

        try {

            gatewayClient.post("/course/courses/" + courseId + "/enroll", null, JsonNode.class);

            return true;

        } catch (Exception e) {

            log.error("Lỗi khi đăng ký khóa học {}: {}", courseId, e.getMessage());

            return false;

        }

    }



    public List<String> getAllCategories() {

        try {

            JsonNode response = gatewayClient.get("/course/categories", JsonNode.class);

            if (response != null && response.has("data") && response.get("data").isArray()) {

                List<String> categories = new ArrayList<>();

                for (JsonNode cat : response.get("data")) {

                    if (cat.has("name")) {

                        categories.add(cat.get("name").asText());

                    }

                }

                if (!categories.isEmpty()) return categories;

            }

        } catch (Exception e) {

            log.warn("Lỗi khi tải danh mục khóa học từ Gateway: {}", e.getMessage());

        }

        return List.of("Công nghệ Thông tin", "Lập trình Web", "Kiến trúc Microservices", "Cơ sở Dữ liệu & Cloud");

    }



    private CourseDto mapJsonToCourseDto(JsonNode node) {

        String id = node.hasNonNull("id") ? node.get("id").asText() : "";

        String title = node.hasNonNull("title") ? node.get("title").asText() : "";

        String desc = node.hasNonNull("description") ? node.get("description").asText() : "";

        String level = node.hasNonNull("level") ? node.get("level").asText() : "BEGINNER";

        String status = node.hasNonNull("status") ? node.get("status").asText() : "PUBLIC";

        String thumb = node.hasNonNull("thumbnailUrl") ? node.get("thumbnailUrl").asText()

                : "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80";



        List<String> outcomes = new ArrayList<>();

        if (node.hasNonNull("learningPoints") && node.get("learningPoints").isArray()) {

            for (JsonNode lp : node.get("learningPoints")) {

                if (lp != null && !lp.isNull() && !lp.asText().isBlank()) {

                    outcomes.add(lp.asText());

                }

            }

        }

        if (outcomes.isEmpty()) {

            outcomes.add("Làm chủ kiến thức nền tảng và thực hành dự án thực tế.");

        }



        List<String> tags = new ArrayList<>();

        if (node.hasNonNull("categories") && node.get("categories").isArray()) {

            for (JsonNode c : node.get("categories")) {

                if (c.hasNonNull("name")) tags.add(c.get("name").asText());

            }

        }



        String primaryCategory = tags.isEmpty() ? "Công nghệ Thông tin" : tags.get(0);



        Double price = 0.0;

        if (node.hasNonNull("price")) {

            price = node.get("price").asDouble();

        } else if ("c3333333-3333-3333-3333-333333333333".equals(id)) {

            price = 799000.0;

        } else if ("c4444444-4444-4444-4444-444444444444".equals(id)) {

            price = 499000.0;

        } else if ("c5555555-5555-5555-5555-555555555555".equals(id)) {

            price = 699000.0;

        }



        String instructorId = node.hasNonNull("instructorId") ? node.get("instructorId").asText() : "";

        String instructorName = "ThS. Nguyễn Minh Trí";

        String instructorTitle = "Giảng viên phụ trách bộ môn";

        if (node.hasNonNull("instructorName") && !node.get("instructorName").asText().isBlank()) {

            instructorName = node.get("instructorName").asText();

        } else if ("33333333-3333-3333-3333-333333333333".equals(instructorId)) {

            instructorName = "ThS. Trần Quốc Bảo";

            instructorTitle = "Chuyên gia Cloud & DevOps Senior";

        } else if ("44444444-4444-4444-4444-444444444444".equals(instructorId)) {

            instructorName = "TS. Lê Thị Mai Linh";

            instructorTitle = "Trưởng Bộ Môn Công Nghệ Phần Mềm";

        }



        return CourseDto.builder()

                .id(id)

                .title(title)

                .description(desc)

                .shortDescription(desc)

                .category(primaryCategory)

                .difficulty(level)

                .thumbnailUrl(thumb)

                .instructorName(instructorName)

                .instructorTitle(instructorTitle)

                .totalLessons(node.hasNonNull("exerciseCount") && node.get("exerciseCount").asInt() > 0 ? node.get("exerciseCount").asInt() : 6)

                .durationHours(node.hasNonNull("duration") ? (node.get("duration").asInt() / 60) : 24)

                .rating("c2222222-2222-2222-2222-222222222222".equals(id) ? 4.8 : (("c3333333-3333-3333-3333-333333333333".equals(id) || "c5555555-5555-5555-5555-555555555555".equals(id)) ? 5.0 : 4.9))

                .reviewsCount(node.hasNonNull("studentCount") && node.get("studentCount").asInt() > 0 ? (node.get("studentCount").asInt() >= 9 ? node.get("studentCount").asInt() - 1 : node.get("studentCount").asInt()) : 0)

                .studentsEnrolled(node.hasNonNull("studentCount") ? node.get("studentCount").asInt() : 0)

                .price(price)

                .tags(tags)

                .outcomes(outcomes)

                .chapters(new ArrayList<>())

                .build();

    }



    private List<ChapterDto> mapStructureNodesToChapters(String courseId, JsonNode nodesArray) {

        Map<String, ChapterDto> chaptersMap = new LinkedHashMap<>();

        Map<String, List<LessonDto>> lessonsByParent = new HashMap<>();



        for (JsonNode node : nodesArray) {

            String type = node.hasNonNull("type") ? node.get("type").asText() : "CHAPTER";

            String nodeId = node.hasNonNull("id") ? node.get("id").asText() : (node.hasNonNull("_id") ? node.get("_id").asText() : UUID.randomUUID().toString());

            String title = node.hasNonNull("title") ? node.get("title").asText() : "Chương";

            String parentId = node.hasNonNull("parentId") ? node.get("parentId").asText() : null;

            int order = node.hasNonNull("order") ? node.get("order").asInt() : 1;



            if ("CHAPTER".equalsIgnoreCase(type) || "FOLDER".equalsIgnoreCase(type) || parentId == null) {

                chaptersMap.put(nodeId, ChapterDto.builder()

                        .id(nodeId)

                        .title(title)

                        .orderIndex(order)

                        .lessons(new ArrayList<>())

                        .build());

            } else {

                String realLessonId = node.hasNonNull("lessonId") ? node.get("lessonId").asText() : nodeId;

                String defaultVideo = LESSON_VIDEOS.getOrDefault(realLessonId, "https://www.youtube.com/watch?v=3c-iBn73dDE");

                String videoUrl = node.hasNonNull("videoUrl") ? node.get("videoUrl").asText() : defaultVideo;

                String desc = node.hasNonNull("description") ? node.get("description").asText() : "Nội dung bài học được lưu trữ đồng bộ từ hệ thống EduLearn LMS.";

                lessonsByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(

                        LessonDto.builder()

                                .id(realLessonId)

                                .courseId(courseId)

                                .chapterId(parentId)

                                .title(title)

                                .orderIndex(order)

                                .type(node.hasNonNull("lessonType") ? node.get("lessonType").asText().toUpperCase() : "VIDEO")

                                .durationMinutes(25)

                                .videoUrl(formatYoutubeUrl(videoUrl))

                                .content(desc)

                                .isPreview(true)

                                .build()

                );

            }

        }



        for (Map.Entry<String, List<LessonDto>> entry : lessonsByParent.entrySet()) {

            ChapterDto chapter = chaptersMap.get(entry.getKey());

            if (chapter != null) {

                chapter.getLessons().addAll(entry.getValue());

            }

        }



        return new ArrayList<>(chaptersMap.values());

    }



    private String formatYoutubeUrl(String url) {

        if (url == null || url.isBlank()) return url;

        url = url.trim();

        if (url.contains("youtube.com/watch?v=")) {

            String id = url.substring(url.indexOf("v=") + 2);

            int amp = id.indexOf('&');

            if (amp != -1) id = id.substring(0, amp);

            return "https://www.youtube.com/embed/" + id;

        } else if (url.contains("youtu.be/")) {

            String id = url.substring(url.lastIndexOf('/') + 1);

            int q = id.indexOf('?');

            if (q != -1) id = id.substring(0, q);

            return "https://www.youtube.com/embed/" + id;

        }

        return url;

    }

}

