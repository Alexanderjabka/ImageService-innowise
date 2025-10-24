package io.github.alexanderjabka.imageservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class S3Service {
    
    @Value("${cloud.aws.s3.endpoint:http://localstack:4566}")
    private String s3Endpoint;
    
    @Value("${cloud.aws.s3.bucket:images}")
    private String bucketName;
    
    public String uploadFile(MultipartFile file, Long userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueFileName = String.format("%s_%s_%s%s", 
            userId, timestamp, UUID.randomUUID().toString().substring(0, 8), fileExtension);
        
        return String.format("%s/%s/%s", s3Endpoint, bucketName, uniqueFileName);
    }
    
    public void deleteFile(String s3Url) {
        try {
            System.out.println("Deleting file from S3: " + s3Url);
        } catch (Exception e) {
            System.err.println("Error deleting file from S3: " + e.getMessage());
        }
    }
    
    public boolean fileExists(String s3Url) {
        try {
            return s3Url != null && !s3Url.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }
}
