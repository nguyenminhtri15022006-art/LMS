/**
 * MONGODB INITIALIZATION SCRIPT CHO EDULEARN LMS
 * CSDL: learning_lms
 * Chứa cấu trúc đề cương cây bài học (CourseStructure) và bài nộp code chi tiết
 */

db = db.getSiblingDB('learning_lms');

// 1. Tạo Collection course_structures
db.course_structures.drop();
db.course_structures.insertMany([
  {
    "_id": "d1111111-1111-1111-1111-111111111111",
    "courseId": "c1111111-1111-1111-1111-111111111111",
    "nodes": [
      {
        "id": "ch-1",
        "type": "CHAPTER",
        "title": "Chương 1: Tổng quan Spring Boot 3 & Kiến trúc Monolith BFF",
        "order": 1,
        "parentId": null
      },
      {
        "id": "les-11111111-1111-1111-1111-111111111111",
        "type": "LESSON",
        "title": "Bài 1.1: Cài đặt JDK 21, IntelliJ IDEA và Khởi tạo dự án",
        "order": 1,
        "parentId": "ch-1",
        "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "durationSeconds": 1200
      },
      {
        "id": "les-11111111-1111-1111-1111-111111111112",
        "type": "LESSON",
        "title": "Bài 1.2: Cấu hình Maven, Thymeleaf & Bootstrap 5",
        "order": 2,
        "parentId": "ch-1",
        "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "durationSeconds": 1500
      },
      {
        "id": "ch-2",
        "type": "CHAPTER",
        "title": "Chương 2: Thiết kế RESTful API & Cơ sở dữ liệu",
        "order": 2,
        "parentId": null
      },
      {
        "id": "les-11111111-1111-1111-1111-111111111113",
        "type": "LESSON",
        "title": "Bài 2.1: Spring Data JPA, Entity Mapping & Flyway Migration",
        "order": 1,
        "parentId": "ch-2",
        "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "durationSeconds": 1800
      }
    ],
    "createdAt": new Date(),
    "updatedAt": new Date()
  },
  {
    "_id": "d2222222-2222-2222-2222-222222222222",
    "courseId": "c2222222-2222-2222-2222-222222222222",
    "nodes": [
      {
        "id": "ch-2-1",
        "type": "CHAPTER",
        "title": "Chương 1: Netflix Eureka Service Discovery & Spring Cloud Gateway",
        "order": 1,
        "parentId": null
      },
      {
        "id": "les-22222222-2222-2222-2222-222222222221",
        "type": "LESSON",
        "title": "Bài 1.1: Đăng ký dịch vụ với Eureka Server và cấu hình Route",
        "order": 1,
        "parentId": "ch-2-1",
        "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "durationSeconds": 1600
      }
    ],
    "createdAt": new Date(),
    "updatedAt": new Date()
  }
]);

print("Đã nạp thành công 2 bộ cấu trúc đề cương vào MongoDB (learning_lms.course_structures)!");
