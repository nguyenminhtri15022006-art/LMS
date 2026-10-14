package com.lms.courseservice.service.impl;

import com.lms.courseservice.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        return "http://localhost:9000/lms-course-bucket/" + folder + "/" + filename;
    }

    @Override
    public void deleteFile(String fileUrl) {
        log.info("Deleting file from MinIO: {}", fileUrl);
    }
}
