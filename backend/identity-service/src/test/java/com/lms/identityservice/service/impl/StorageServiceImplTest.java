package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.response.PresignedUrlResponse;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    @Mock
    MinioClient minioClient;

    StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageServiceImpl(minioClient);
        ReflectionTestUtils.setField(storageService, "bucketName", "lms-identity-bucket");
        ReflectionTestUtils.setField(storageService, "minioUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(storageService, "minioPublicUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(storageService, "maxFileSizeBytes", 5242880L);
        ReflectionTestUtils.setField(storageService, "presignedExpiryMinutes", 15L);

        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication("user-1", "student@example.com", "ROLE_STUDENT", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generatePresignedUrlSuccessWithPostPolicyAndBucketUploadUrl() throws Exception {
        when(minioClient.getPresignedPostFormData(any(PostPolicy.class)))
                .thenReturn(Map.of(
                        "policy", "sample-policy-base64",
                        "x-amz-algorithm", "AWS4-HMAC-SHA256",
                        "x-amz-signature", "sample-signature"
                ));

        PresignedUrlResponse response = storageService.generatePresignedUrl("avatar.png", "image/png", "avatars");

        assertThat(response).isNotNull();
        // uploadUrl must be Bucket URL for browser fetch(uploadUrl, { method: 'POST', body: formData })
        assertThat(response.getUploadUrl()).isEqualTo("http://localhost:9000/lms-identity-bucket");
        assertThat(response.getFormFields()).containsKey("key");
        assertThat(response.getFormFields().get("key")).startsWith("avatars/user-1-");
        assertThat(response.getFormFields().get("key")).endsWith(".png");
        assertThat(response.getFormFields()).containsEntry("policy", "sample-policy-base64");
        assertThat(response.getFormFields()).containsEntry("x-amz-signature", "sample-signature");
        assertThat(response.getPublicUrl()).isEqualTo("http://localhost:9000/lms-identity-bucket/" + response.getFormFields().get("key"));

        // Verify MinIO PostPolicy was generated, not presigned PUT url
        verify(minioClient).getPresignedPostFormData(any(PostPolicy.class));
        verify(minioClient, never()).getPresignedObjectUrl(any());
    }

    @Test
    void generatePresignedUrlThrowsExceptionWhenMinioFails() throws Exception {
        when(minioClient.getPresignedPostFormData(any(PostPolicy.class)))
                .thenThrow(new RuntimeException("MinIO network failure"));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> storageService.generatePresignedUrl("avatar.png", "image/png", "avatars"))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void generatePresignedUrlThrowsWhenFileExtensionUnsupported() {
        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> storageService.generatePresignedUrl("malicious.exe", "application/x-msdownload", "avatars"))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_TYPE_NOT_SUPPORTED));

        verifyNoInteractions(minioClient);
    }

    @Test
    void deleteFileCallsMinioRemoveObject() throws Exception {
        storageService.deleteFile("http://localhost:9000/lms-identity-bucket/avatars/avatar-1.png");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
