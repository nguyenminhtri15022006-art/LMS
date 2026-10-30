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
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.enums.NodeType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CourseStructureMapper;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.service.CourseStructureService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseStructureServiceImpl implements CourseStructureService {

    CourseRepository courseRepository;
    CourseStructureRepository courseStructureRepository;
    LessonRepository lessonRepository;
    CourseStructureMapper courseStructureMapper;

    @Override
    public CourseStructureResponse getStructure(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        CourseStructure courseStructure = findStructureByCourse(course);

        CourseStructureResponse response = courseStructureMapper.toCourseStructureResponse(courseStructure);

        if (courseStructure.getNodes() != null) {
            List<String> lessonIds = courseStructure.getNodes().stream()
                    .filter(n -> NodeType.LESSON.equals(n.getType()) && n.getLessonId() != null)
                    .map(StructureNode::getLessonId)
                    .collect(Collectors.toList());

            if (!lessonIds.isEmpty()) {
                Map<String, LessonType> lessonTypeMap = lessonRepository.findAllById(lessonIds).stream()
                        .filter(l -> l.getLessonType() != null)
                        .collect(Collectors.toMap(Lesson::getId, Lesson::getLessonType));

                if (response.getNodes() != null) {
                    response.getNodes().forEach(node -> {
                        if (NodeType.LESSON.equals(node.getType()) && node.getLessonId() != null) {
                            node.setLessonType(lessonTypeMap.get(node.getLessonId()));
                        }
                    });
                }
            }
        }

        return response;
    }

    @Override
    public CourseStructureResponse addNode(String courseId, AddNodeRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        CourseStructure structure = findStructureByCourse(course);

        if (request.getParentId() != null) {
            validateParentExists(structure.getNodes(), request.getParentId());
        }

        StructureNode newNode = StructureNode.builder()
                .id(UUID.randomUUID().toString())
                .type(request.getType())
                .title(request.getTitle())
                .parentId(request.getParentId())
                .order(request.getOrder() != null ? request.getOrder()
                        : getNextOrder(structure.getNodes(), request.getParentId()))
                .build();

        if (NodeType.LESSON.equals(request.getType())) {
            Lesson lesson = Lesson.builder()
                    .id(UUID.randomUUID().toString())
                    .courseId(courseId)
                    .content(new HashMap<>())
                    .build();
            Lesson savedLesson = lessonRepository.save(lesson);
            newNode.setLessonId(savedLesson.getId());
        }

        if (structure.getNodes() == null) {
            structure.setNodes(new ArrayList<>());
        }
        structure.getNodes().add(newNode);
        structure.setUpdatedAt(LocalDateTime.now());

        CourseStructure saved = courseStructureRepository.save(structure);
        return courseStructureMapper.toCourseStructureResponse(saved);
    }

    @Override
    public CourseStructureResponse addNodes(String courseId, AddMultipleNodesRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        CourseStructure structure = findStructureByCourse(course);
        if (structure.getNodes() == null) {
            structure.setNodes(new ArrayList<>());
        }

        for (AddNodeRequest nodeRequest : request.getNodes()) {
            if (nodeRequest.getParentId() != null) {
                validateParentExists(structure.getNodes(), nodeRequest.getParentId());
            }

            StructureNode newNode = StructureNode.builder()
                    .id(UUID.randomUUID().toString())
                    .type(nodeRequest.getType())
                    .title(nodeRequest.getTitle())
                    .parentId(nodeRequest.getParentId())
                    .order(nodeRequest.getOrder() != null ? nodeRequest.getOrder()
                            : getNextOrder(structure.getNodes(), nodeRequest.getParentId()))
                    .build();

            if (NodeType.LESSON.equals(nodeRequest.getType())) {
                Lesson lesson = Lesson.builder()
                        .id(UUID.randomUUID().toString())
                        .courseId(courseId)
                        .content(new HashMap<>())
                        .build();
                Lesson savedLesson = lessonRepository.save(lesson);
                newNode.setLessonId(savedLesson.getId());
            }

            structure.getNodes().add(newNode);
        }

        structure.setUpdatedAt(LocalDateTime.now());
        CourseStructure saved = courseStructureRepository.save(structure);
        return courseStructureMapper.toCourseStructureResponse(saved);
    }

    @Override
    public CourseStructureResponse reorderNodes(String courseId, ReorderNodesRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        CourseStructure structure = findStructureByCourse(course);
        if (structure.getNodes() == null) {
            structure.setNodes(new ArrayList<>());
        }

        Map<String, StructureNode> nodeMap = structure.getNodes().stream()
                .collect(Collectors.toMap(StructureNode::getId, node -> node));

        for (ReorderNodesRequest.NodeOrderItem item : request.getNodes()) {
            StructureNode node = nodeMap.get(item.getNodeId());
            if (node == null) {
                throw new CourseException(ErrorCode.STRUCTURE_NODE_NOT_FOUND);
            }
            node.setOrder(item.getOrder());
            node.setParentId(item.getParentId());
        }

        structure.setUpdatedAt(LocalDateTime.now());
        CourseStructure saved = courseStructureRepository.save(structure);
        return courseStructureMapper.toCourseStructureResponse(saved);
    }

    @Override
    public CourseStructureResponse updateNodeTitle(String courseId, String nodeId, UpdateNodeTitleRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        CourseStructure structure = findStructureByCourse(course);
        if (structure.getNodes() == null) {
            throw new CourseException(ErrorCode.STRUCTURE_NODE_NOT_FOUND);
        }

        StructureNode target = structure.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new CourseException(ErrorCode.STRUCTURE_NODE_NOT_FOUND));

        target.setTitle(request.getTitle());
        structure.setUpdatedAt(LocalDateTime.now());

        CourseStructure saved = courseStructureRepository.save(structure);
        return courseStructureMapper.toCourseStructureResponse(saved);
    }

    @Override
    public void deleteNode(String courseId, String nodeId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        CourseStructure structure = findStructureByCourse(course);
        if (structure.getNodes() == null) {
            throw new CourseException(ErrorCode.STRUCTURE_NODE_NOT_FOUND);
        }

        StructureNode targetNode = structure.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new CourseException(ErrorCode.STRUCTURE_NODE_NOT_FOUND));

        List<String> nodeIdsToDelete = getNodeIdsToDelete(structure.getNodes(), targetNode.getId());

        structure.getNodes().stream()
                .filter(n -> nodeIdsToDelete.contains(n.getId())
                        && NodeType.LESSON.equals(n.getType())
                        && n.getLessonId() != null)
                .forEach(n -> lessonRepository.deleteById(n.getLessonId()));

        structure.getNodes().removeIf(n -> nodeIdsToDelete.contains(n.getId()));
        structure.setUpdatedAt(LocalDateTime.now());

        courseStructureRepository.save(structure);
    }

    @Override
    public int countLessons(String courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return 0;
        CourseStructure structure = findStructureByCourse(course);
        if (structure.getNodes() == null) return 0;
        return (int) structure.getNodes().stream()
                .filter(node -> NodeType.LESSON.equals(node.getType()))
                .count();
    }

    @Override
    public boolean isLessonInCourse(String courseId, String lessonId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return false;
        CourseStructure structure = findStructureByCourse(course);
        if (structure.getNodes() == null) return false;
        return structure.getNodes().stream()
                .anyMatch(node -> NodeType.LESSON.equals(node.getType())
                        && (lessonId.equals(node.getLessonId()) || lessonId.equals(node.getId())));
    }

    @Override
    public List<StructureNodeResponse> getLessonNodes(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        CourseStructure structure = findStructureByCourse(course);

        if (structure.getNodes() == null) return Collections.emptyList();

        return structure.getNodes().stream()
                .filter(node -> NodeType.LESSON.equals(node.getType()))
                .sorted(Comparator.comparing(node -> node.getOrder() != null ? node.getOrder() : 0))
                .map(courseStructureMapper::toNodeResponse)
                .toList();
    }

    // ===== Helpers =====

    private CourseStructure findStructureByCourse(Course course) {
        return courseStructureRepository.findByCourseId(course.getId())
                .or(() -> {
                    if (course.getMongoStructureId() != null) {
                        return courseStructureRepository.findById(course.getMongoStructureId());
                    }
                    return Optional.empty();
                })
                .orElseGet(() -> {
                    CourseStructure structure = CourseStructure.builder()
                            .courseId(course.getId())
                            .nodes(new java.util.ArrayList<>())
                            .build();
                    structure = courseStructureRepository.save(structure);
                    course.setMongoStructureId(structure.getId());
                    courseRepository.save(course);
                    return structure;
                });
    }

    private void checkCourseOwner(Course course) {
        String userId = GatewayAuthentication.currentUserId();
        String role = GatewayAuthentication.currentRole();
        if ("ROLE_ADMIN".equals(role) || "ADMIN".equals(role)) return;
        if (!course.getInstructorId().equals(userId)) {
            throw new CourseException(ErrorCode.NOT_COURSE_OWNER);
        }
    }

    private void checkNotLocked(Course course) {
        if (CourseStatus.LOCKED.equals(course.getStatus())) {
            throw new CourseException(ErrorCode.COURSE_LOCKED);
        }
    }

    private void validateParentExists(List<StructureNode> nodes, String parentId) {
        if (nodes == null) throw new CourseException(ErrorCode.PARENT_NODE_NOT_FOUND);
        boolean exists = nodes.stream().anyMatch(n -> n.getId().equals(parentId));
        if (!exists) {
            throw new CourseException(ErrorCode.PARENT_NODE_NOT_FOUND);
        }
    }

    private int getNextOrder(List<StructureNode> nodes, String parentId) {
        if (nodes == null) return 0;
        return (int) nodes.stream()
                .filter(n -> Objects.equals(n.getParentId(), parentId))
                .count();
    }

    private List<String> getNodeIdsToDelete(List<StructureNode> nodes, String rootNodeId) {
        List<String> result = new ArrayList<>();
        result.add(rootNodeId);

        List<String> children = nodes.stream()
                .filter(n -> rootNodeId.equals(n.getParentId()))
                .map(StructureNode::getId)
                .toList();

        for (String childId : children) {
            result.addAll(getNodeIdsToDelete(nodes, childId));
        }

        return result;
    }
}
