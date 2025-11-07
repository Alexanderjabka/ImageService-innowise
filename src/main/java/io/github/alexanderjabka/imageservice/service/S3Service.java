package io.github.alexanderjabka.imageservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class S3Service {
    private final AmazonS3 s3;
    private final String bucket;

    public S3Service(AmazonS3 s3, @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String upload(MultipartFile file) throws IOException {
        String key = UUID.randomUUID() + "/" + file.getOriginalFilename();
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentType(file.getContentType());
        meta.setContentLength(file.getSize());
        try (InputStream is = file.getInputStream()) {
            PutObjectRequest req = new PutObjectRequest(bucket, key, is, meta)
                    .withCannedAcl(CannedAccessControlList.Private);
            s3.putObject(req);
        }
        return key;
    }

    public S3Object getObject(String key) {
        return s3.getObject(new GetObjectRequest(bucket, key));
    }

    public void delete(String key) {
        s3.deleteObject(bucket, key);
    }
}
