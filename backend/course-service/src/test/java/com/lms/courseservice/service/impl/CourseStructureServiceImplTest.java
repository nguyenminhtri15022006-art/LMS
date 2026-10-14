package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.AddMultipleNodesRequest;
import com.lms.courseservice.dto.request.AddNodeRequest;
import com.lms.courseservice.dto.request.ReorderNodesRequest;
import com.lms.courseservice.dto.request.UpdateNodeTitleRequest;
import com.lms.courseservice.dto.response.CourseStructureResponse;
import com.lms.courseservice.dto.response.StructureNodeResponse;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mongo.Lesson;
import com.lms.courseservice.entity.mongo.StructureNode;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.NodeType;
import com.lms.courseservice.mapper.CourseStructureMapper;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseStructureServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseStructureRepository courseStructureRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseStructureMapper courseStructureMapper;

    @InjectMocks
    private CourseStructureServiceImpl courseStructureService;

    private final String instructorId = "inst-1111-2222";
    private final String courseId = "course-uuid-1234";
    private final String structureId = "structure-uuid-5678";

    private Course testCourse;
    private CourseStructure testStructure;
    private CourseStructureResponse testResponse;

    @BeforeEach
    void setUp() {
        testCourse = Course.builder()
                .id(courseId)
                .title("Spring Boot")
                .instructorId(instructorId)
                .status(CourseStatus.PRIVATE)
                .mongoStructureId(structureId)
                .build();

        StructureNode rootFolder = StructureNode.builder()
                .id("node-folder-1")
                .title("Chương 1: Giới thiệu")
                .type(NodeType.FOLDER)
                .order(0)
                .parentId(null)
                .build();

        StructureNode lessonNode = StructureNode.builder()
                .id("node-lesson-1")
                .title("Bài 1.1: Khởi động")
                .type(NodeType.LESSON)
                .order(0)
                .parentId("node-folder-1")
                .lessonId("lesson-mongo-1")
                .build();

        testStructure = CourseStructure.builder()
                .id(structureId)
                .courseId(courseId)
                .nodes(new ArrayList<>(List.of(rootFolder, lessonNode)))
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = CourseStructureResponse.builder()
                .id(structureId)
                .courseId(courseId)
                .nodes(List.of(
                        StructureNodeResponse.builder().id("node-folder-1").title("Chương 1: Giới thiệu").type(NodeType.FOLDER).build(),
                        StructureNodeResponse.builder().id("node-lesson-1").title("Bài 1.1: Khởi động").type(NodeType.LESSON).build()
                ))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication(instructorId, "instructor@example.com", "ROLE_INSTRUCTOR",
                        List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("13. getStructureExistingCourseReturnsMongoTree: Lấy cây cấu trúc bài giảng từ MongoDB course_structures")
    void getStructureExistingCourseReturnsMongoTree() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseStructureRepository.findByCourseId(courseId)).thenReturn(Optional.of(testStructure));
        when(courseStructureMapper.toCourseStructureResponse(testStructure)).thenReturn(testResponse);
        when(lessonRepository.findAllById(any())).thenReturn(List.of());

        CourseStructureResponse result = courseStructureService.getStructure(courseId);

        assertNotNull(result);
        assertEquals(courseId, result.getCourseId());
        assertEquals(2, result.getNodes().size());
        verify(courseStructureRepository).findByCourseId(courseId);
    }

    @Test
    @DisplayName("14. addNodeValidParentAddsNodeToTree: Thêm node chương mục mới vào cây thành công")
    void addNodeValidParentAddsNodeToTree() {
        AddNodeRequest request = AddNodeRequest.builder()
                .type(NodeType.LESSON)
                .title("Bài 1.2: Cài đặt JDK")
                .parentId("node-folder-1")
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseStructureRepository.findByCourseId(courseId)).thenReturn(Optional.of(testStructure));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(Lesson.builder().id("new-lesson-id").build());
        when(courseStructureRepository.save(any(CourseStructure.class))).thenReturn(testStructure);
        when(courseStructureMapper.toCourseStructureResponse(any())).thenReturn(testResponse);

        CourseStructureResponse result = courseStructureService.addNode(courseId, request);

        assertNotNull(result);
        verify(courseStructureRepository).save(any(CourseStructure.class));
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("15. addMultipleNodesBatchSuccess: Thêm hàng loạt node vào cây cấu trúc thành công")
    void addMultipleNodesBatchSuccess() {
        AddMultipleNodesRequest request = AddMultipleNodesRequest.builder()
                .nodes(List.of(
                        AddNodeRequest.builder().type(NodeType.FOLDER).title("Chương 2").build(),
                        AddNodeRequest.builder().type(NodeType.LESSON).title("Bài 2.1").build()
                ))
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseStructureRepository.findByCourseId(courseId)).thenReturn(Optional.of(testStructure));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(Lesson.builder().id("lesson-batch-1").build());
        when(courseStructureRepository.save(any(CourseStructure.class))).thenReturn(testStructure);
        when(courseStructureMapper.toCourseStructureResponse(any())).thenReturn(testResponse);

        CourseStructureResponse result = courseStructureService.addNodes(courseId, request);

        assertNotNull(result);
        verify(courseStructureRepository).save(any(CourseStructure.class));
    }

    @Test
    @DisplayName("16. reorderNodesValidListUpdatesOrderInMongo: Kéo thả cập nhật thứ tự các node trong MongoDB")
    void reorderNodesValidListUpdatesOrderInMongo() {
        ReorderNodesRequest request = ReorderNodesRequest.builder()
                .nodes(List.of(
                        new ReorderNodesRequest.NodeOrderItem("node-folder-1", null, 1),
                        new ReorderNodesRequest.NodeOrderItem("node-lesson-1", "node-folder-1", 0)
                ))
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseStructureRepository.findByCourseId(courseId)).thenReturn(Optional.of(testStructure));
        when(courseStructureRepository.save(any(CourseStructure.class))).thenReturn(testStructure);
        when(courseStructureMapper.toCourseStructureResponse(any())).thenReturn(testResponse);

        CourseStructureResponse result = courseStructureService.reorderNodes(courseId, request);

        assertNotNull(result);
        verify(courseStructureRepository).save(testStructure);
    }

    @Test
    @DisplayName("17. updateNodeTitleExistingNodeUpdatesTitle: Cập nhật tiêu đề node thành công")
    void updateNodeTitleExistingNodeUpdatesTitle() {
        UpdateNodeTitleRequest request = UpdateNodeTitleRequest.builder()
                .title("Chương 1: Tổng quan đổi tên")
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseStructureRepository.findByCourseId(courseId)).thenReturn(Optional.of(testStructure));
        when(courseStructureRepository.save(any(CourseStructure.class))).thenReturn(testStructure);
        when(courseStructureMapper.toCourseStructureResponse(any())).thenReturn(testResponse);

        CourseStructureResponse result = courseStructureService.updateNodeTitle(courseId, "node-folder-1", request);

        assertNotNull(result);
        verify(courseStructureRepository).save(testStructure);
    }

    @Test
    @DisplayName("18. deleteNodeRemovesNodeAndCleansChildren: Xóa node khỏi cây và dọn dẹp các node con")
    void deleteNodeRemovesNodeAndCleansChildren() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseStructureRepository.findByCourseId(courseId)).thenReturn(Optional.of(testStructure));

        assertDoesNotThrow(() -> courseStructureService.deleteNode(courseId, "node-folder-1"));

        // Deleting folder-1 also cleans its child lesson-1 and deletes its lesson from Mongo
        verify(lessonRepository).deleteById("lesson-mongo-1");
        verify(courseStructureRepository).save(testStructure);
        assertTrue(testStructure.getNodes().isEmpty());
    }
}
