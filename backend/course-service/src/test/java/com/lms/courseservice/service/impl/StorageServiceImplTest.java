package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.response.CompleteMultipartResponse;
import com.lms.courseservice.dto.response.InitiateMultipartResponse;
import com.lms.courseservice.dto.response.PresignedUrlResponse;
import com.lms.courseservice.service.MinioService;
import io.minio.ComposeObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    @Mock
    private MinioClient publicMinioClient;

    @Mock
    private MinioClient internalMinioClient;

    @Mock
    private MinioService minioService;

    private StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageServiceImpl(publicMinioClient, internalMinioClient, minioService);
        ReflectionTestUtils.setField(storageService, "bucketName", "lms-course-bucket");
        ReflectionTestUtils.setField(storageService, "minioUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(storageService, "minioPublicUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(storageService, "presignedExpiryMinutes", 60);
    }

    @Test
    @DisplayName("11. generatePresignedUrlSuccess: Tạo Presigned URL upload tài liệu trực tiếp lên MinIO")
    void generatePresignedUrlSuccess() throws Exception {
        when(publicMinioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/lms-course-bucket/documents/uuid.pdf?signature=xyz");

        PresignedUrlResponse response = storageService.generatePresignedUrl("document.pdf", "application/pdf", "documents");

        assertThat(response).isNotNull();
        assertThat(response.getPresignedUrl()).contains("signature=xyz");
        assertThat(response.getPublicUrl()).contains("http://localhost:9000/lms-course-bucket/documents/");
        assertThat(response.getPublicUrl()).endsWith(".pdf");
    }

    @Test
    @DisplayName("12. initiateMultipartUploadLargeVideoReturnsUploadId: Khởi tạo tải lên video nhiều phần MinIO thành công")
    void initiateMultipartUploadLargeVideoReturnsUploadId() throws Exception {
        when(publicMinioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/part-url");

        InitiateMultipartResponse response = storageService.initiateMultipartUpload(
                "large-video.mp4", "video/mp4", "videos", 3
        );

        assertThat(response).isNotNull();
        assertThat(response.getUploadId()).isNotBlank();
        assertThat(response.getObjectName()).contains("videos/");
        assertThat(response.getObjectName()).endsWith(".mp4");
        assertThat(response.getPresignedUrls()).hasSize(3);

        verify(publicMinioClient, times(3)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    @DisplayName("13. completeMultipartUploadValidPartsCombinesFile: Hoàn tất ghép nối các phần video trên MinIO")
    void completeMultipartUploadValidPartsCombinesFile() throws Exception {
        CompleteMultipartResponse response = storageService.completeMultipartUpload(
                "upload-123", "videos/final.mp4", 3
        );

        assertThat(response).isNotNull();
        assertThat(response.getPublicUrl()).isEqualTo("http://localhost:9000/lms-course-bucket/videos/final.mp4");

        verify(internalMinioClient).composeObject(any(ComposeObjectArgs.class));
        verify(internalMinioClient, times(3)).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("14. deleteFileValidUrlDeletesFromMinio: Xóa file khỏi MinIO bucket thành công")
    void deleteFileValidUrlDeletesFromMinio() {
        String fileUrl = "http://localhost:9000/lms-course-bucket/videos/sample.mp4";

        storageService.deleteFile(fileUrl);

        verify(minioService).deleteFile(fileUrl);
    }
}
