package io.github.alexanderjabka.imageservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private AmazonS3 s3Client;

    private S3Service s3Service;

    private static final String TEST_BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, TEST_BUCKET);
    }

    @Test
    void upload_ShouldUploadFileToS3_WhenValidFileProvided() throws IOException {
        // Given
        byte[] content = "test image content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                content
        );

        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());

        // When
        String key = s3Service.upload(file);

        // Then
        assertThat(key).isNotNull();
        assertThat(key).contains("test-image.jpg");
        assertThat(key).matches("^[0-9a-f-]+/test-image\\.jpg$");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(captor.capture());

        PutObjectRequest request = captor.getValue();
        assertThat(request.getBucketName()).isEqualTo(TEST_BUCKET);
        assertThat(request.getKey()).isEqualTo(key);
        assertThat(request.getMetadata().getContentType()).isEqualTo("image/jpeg");
        assertThat(request.getMetadata().getContentLength()).isEqualTo(content.length);
    }

    @Test
    void upload_ShouldGenerateUniqueKeys_WhenCalledMultipleTimes() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());

        // When
        String key1 = s3Service.upload(file);
        String key2 = s3Service.upload(file);

        // Then
        assertThat(key1).isNotEqualTo(key2);
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class));
    }

    @Test
    void upload_ShouldThrowIOException_WhenFileReadFails() throws IOException {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(100L);
        when(file.getInputStream()).thenThrow(new IOException("Failed to read file"));

        // When & Then
        assertThatThrownBy(() -> s3Service.upload(file))
                .isInstanceOf(IOException.class)
                .hasMessage("Failed to read file");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    void getObject_ShouldRetrieveObjectFromS3_WhenValidKeyProvided() {
        // Given
        String key = "test-key/test-image.jpg";
        S3Object s3Object = mock(S3Object.class);

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);

        // When
        S3Object result = s3Service.getObject(key);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(s3Object);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client, times(1)).getObject(captor.capture());

        GetObjectRequest request = captor.getValue();
        assertThat(request.getBucketName()).isEqualTo(TEST_BUCKET);
        assertThat(request.getKey()).isEqualTo(key);
    }

    @Test
    void delete_ShouldDeleteObjectFromS3_WhenValidKeyProvided() {
        // Given
        String key = "test-key/test-image.jpg";
        doNothing().when(s3Client).deleteObject(anyString(), anyString());

        // When
        s3Service.delete(key);

        // Then
        verify(s3Client, times(1)).deleteObject(eq(TEST_BUCKET), eq(key));
    }

    @Test
    void delete_ShouldHandleMultipleDeletes() {
        // Given
        String key1 = "key1/image1.jpg";
        String key2 = "key2/image2.jpg";
        doNothing().when(s3Client).deleteObject(anyString(), anyString());

        // When
        s3Service.delete(key1);
        s3Service.delete(key2);

        // Then
        verify(s3Client).deleteObject(TEST_BUCKET, key1);
        verify(s3Client).deleteObject(TEST_BUCKET, key2);
        verify(s3Client, times(2)).deleteObject(anyString(), anyString());
    }

    @Test
    void upload_ShouldHandleDifferentContentTypes() throws IOException {
        // Given
        MultipartFile pngFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "png content".getBytes()
        );

        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());

        // When
        String pngKey = s3Service.upload(pngFile);
        String pdfKey = s3Service.upload(pdfFile);

        // Then
        assertThat(pngKey).contains("test.png");
        assertThat(pdfKey).contains("document.pdf");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(2)).putObject(captor.capture());

        assertThat(captor.getAllValues().get(0).getMetadata().getContentType()).isEqualTo("image/png");
        assertThat(captor.getAllValues().get(1).getMetadata().getContentType()).isEqualTo("application/pdf");
    }
}
