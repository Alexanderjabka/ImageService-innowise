package io.github.alexanderjabka.imageservice.service;

import io.github.alexanderjabka.imageservice.dto.CommentResponse;
import io.github.alexanderjabka.imageservice.dto.ImageMetadataResponse;
import io.github.alexanderjabka.imageservice.dto.LikeToggleResponse;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.entity.Comment;
import io.github.alexanderjabka.imageservice.entity.Image;
import io.github.alexanderjabka.imageservice.entity.Like;
import io.github.alexanderjabka.imageservice.repository.CommentRepository;
import io.github.alexanderjabka.imageservice.repository.ImageRepository;
import io.github.alexanderjabka.imageservice.repository.LikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ImageEventsProducer imageEventsProducer;

    @InjectMocks
    private ImageService imageService;

    private Image testImage;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_IMAGE_ID = 100L;
    private static final String TEST_USERNAME = "test-user";

    @BeforeEach
    void setUp() {
        testImage = new Image();
        testImage.setId(TEST_IMAGE_ID);
        testImage.setUrl("test-key/test-image.jpg");
        testImage.setDescription("Test Description");
        testImage.setUploadedAt(Instant.now());
        testImage.setUserId(TEST_USER_ID);
    }

    @Test
    void uploadImage_ShouldUploadAndSaveImage_WhenValidDataProvided() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );
        String description = "Test image";
        String s3Key = "uuid/test.jpg";

        when(s3Service.upload(file)).thenReturn(s3Key);
        when(imageRepository.save(any(Image.class))).thenReturn(testImage);

        // When
        ImageMetadataResponse response = imageService.uploadImage(file, description, TEST_USER_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(TEST_IMAGE_ID);
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);

        verify(s3Service, times(1)).upload(file);

        ArgumentCaptor<Image> imageCaptor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository, times(1)).save(imageCaptor.capture());

        Image savedImage = imageCaptor.getValue();
        assertThat(savedImage.getUrl()).isEqualTo(s3Key);
        assertThat(savedImage.getDescription()).isEqualTo(description);
        assertThat(savedImage.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedImage.getUploadedAt()).isNotNull();
    }

    @Test
    void uploadImage_ShouldPropagateIOException_WhenS3UploadFails() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        when(s3Service.upload(file)).thenThrow(new IOException("S3 upload failed"));

        // When & Then
        assertThatThrownBy(() -> imageService.uploadImage(file, "description", TEST_USER_ID))
                .isInstanceOf(IOException.class)
                .hasMessage("S3 upload failed");

        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    void getImageEntity_ShouldReturnImage_WhenImageExists() {
        // Given
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));

        // When
        Image result = imageService.getImageEntity(TEST_IMAGE_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_IMAGE_ID);
        verify(imageRepository, times(1)).findById(TEST_IMAGE_ID);
    }

    @Test
    void getImageEntity_ShouldThrowException_WhenImageNotFound() {
        // Given
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> imageService.getImageEntity(TEST_IMAGE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Image not found with id:");

        verify(imageRepository, times(1)).findById(TEST_IMAGE_ID);
    }

    @Test
    void getImageMetadata_ShouldReturnMetadata_WhenImageExists() {
        // Given
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));

        // When
        ImageMetadataResponse response = imageService.getImageMetadata(TEST_IMAGE_ID, TEST_USER_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(TEST_IMAGE_ID);
        assertThat(response.getDescription()).isEqualTo(testImage.getDescription());
        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getContentUrl()).isEqualTo("/images/" + TEST_IMAGE_ID + "/content");
    }

    @Test
    void getAllImages_ShouldReturnPageOfImages() {
        // Given
        Image image1 = createTestImage(1L, 10L);
        Image image2 = createTestImage(2L, 20L);
        List<Image> images = Arrays.asList(image1, image2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Image> imagePage = new PageImpl<>(images, pageable, images.size());

        when(imageRepository.findAll(pageable)).thenReturn(imagePage);

        // When
        Page<ImageMetadataResponse> result = imageService.getAllImages(pageable, TEST_USER_ID);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);
    }

    @Test
    void getUserImages_ShouldReturnUserImagesOnly() {
        // Given
        Image image1 = createTestImage(1L, TEST_USER_ID);
        Image image2 = createTestImage(2L, TEST_USER_ID);
        List<Image> images = Arrays.asList(image1, image2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Image> imagePage = new PageImpl<>(images, pageable, images.size());

        when(imageRepository.findByUserId(TEST_USER_ID, pageable)).thenReturn(imagePage);

        // When
        Page<ImageMetadataResponse> result = imageService.getUserImages(TEST_USER_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(img -> img.getUserId().equals(TEST_USER_ID));
        verify(imageRepository, times(1)).findByUserId(TEST_USER_ID, pageable);
    }

    @Test
    void getS3Key_ShouldReturnKey_WhenImageExists() {
        // Given
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));

        // When
        String key = imageService.getS3Key(TEST_IMAGE_ID);

        // Then
        assertThat(key).isEqualTo(testImage.getUrl());
    }

    @Test
    void deleteImage_ShouldDeleteImage_WhenUserIsOwner() {
        // Given
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));
        doNothing().when(s3Service).delete(anyString());
        doNothing().when(imageRepository).deleteById(TEST_IMAGE_ID);

        // When
        imageService.deleteImage(TEST_IMAGE_ID, TEST_USER_ID);

        // Then
        verify(s3Service, times(1)).delete(testImage.getUrl());
        verify(imageRepository, times(1)).deleteById(TEST_IMAGE_ID);
    }

    @Test
    void deleteImage_ShouldThrowException_WhenUserIsNotOwner() {
        // Given
        Long differentUserId = 999L;
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));

        // When & Then
        assertThatThrownBy(() -> imageService.deleteImage(TEST_IMAGE_ID, differentUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You can only delete your own images");

        verify(s3Service, never()).delete(anyString());
        verify(imageRepository, never()).deleteById(any());
    }

    @Test
    void getUserImagesWithPagination_ShouldHandleDefaultParameters() {
        // Given
        Image image = createTestImage(1L, TEST_USER_ID);
        Pageable expectedPageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<Image> imagePage = new PageImpl<>(Arrays.asList(image), expectedPageable, 1);

        when(imageRepository.findByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(imagePage);

        // When
        PageResponse<ImageMetadataResponse> result = imageService.getUserImagesWithPagination(
                TEST_USER_ID, 1, null, null, "uploadedAt", "desc"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(12);
        assertThat(result.getTotalItems()).isEqualTo(1);
    }

    @Test
    void getAllImagesWithPagination_ShouldHandleCustomSorting() {
        // Given
        Image image = createTestImage(1L, TEST_USER_ID);
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "uploadedAt"));
        Page<Image> imagePage = new PageImpl<>(Arrays.asList(image), expectedPageable, 1);

        when(imageRepository.findAll(any(Pageable.class))).thenReturn(imagePage);

        // When
        PageResponse<ImageMetadataResponse> result = imageService.getAllImagesWithPagination(
                TEST_USER_ID, 1, 20, null, "uploadedAt", "asc"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(20);
        verify(imageRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void toggleLike_ShouldAddLike_WhenNoExistingLike() {
        // Given
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(likeRepository.findByImageIdAndUserId(TEST_IMAGE_ID, TEST_USER_ID)).thenReturn(Optional.empty());
        when(likeRepository.save(any(Like.class))).thenReturn(new Like());

        // When
        when(likeRepository.countByImageId(TEST_IMAGE_ID)).thenReturn(1L);

        LikeToggleResponse response = imageService.toggleLike(TEST_IMAGE_ID, TEST_USER_ID);

        // Then
        ArgumentCaptor<Like> likeCaptor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepository, times(1)).save(likeCaptor.capture());

        Like savedLike = likeCaptor.getValue();
        assertThat(savedLike.getImageId()).isEqualTo(TEST_IMAGE_ID);
        assertThat(savedLike.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedLike.getCreatedAt()).isNotNull();
        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikesCount()).isEqualTo(1L);
    }

    @Test
    void toggleLike_ShouldRemoveLike_WhenLikeExists() {
        // Given
        Like existingLike = new Like();
        existingLike.setId(1L);
        existingLike.setImageId(TEST_IMAGE_ID);
        existingLike.setUserId(TEST_USER_ID);

        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(likeRepository.findByImageIdAndUserId(TEST_IMAGE_ID, TEST_USER_ID)).thenReturn(Optional.of(existingLike));
        doNothing().when(likeRepository).delete(existingLike);

        // When
        when(likeRepository.countByImageId(TEST_IMAGE_ID)).thenReturn(0L);

        LikeToggleResponse response = imageService.toggleLike(TEST_IMAGE_ID, TEST_USER_ID);

        // Then
        verify(likeRepository, times(1)).delete(existingLike);
        verify(likeRepository, never()).save(any(Like.class));
        assertThat(response.isLiked()).isFalse();
    }

    @Test
    void addComment_ShouldCreateComment_WhenValidDataProvided() {
        // Given
        String commentText = "Great image!";
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        CommentResponse response = imageService.addComment(TEST_IMAGE_ID, TEST_USER_ID, TEST_USERNAME, commentText);

        // Then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository, times(1)).save(commentCaptor.capture());

        Comment savedComment = commentCaptor.getValue();
        assertThat(savedComment.getImageId()).isEqualTo(TEST_IMAGE_ID);
        assertThat(savedComment.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedComment.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(savedComment.getText()).isEqualTo(commentText);
        assertThat(savedComment.getCreatedAt()).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    void addComment_ShouldThrowException_WhenImageNotFound() {
        // Given
        when(imageRepository.findById(TEST_IMAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> imageService.addComment(TEST_IMAGE_ID, TEST_USER_ID, TEST_USERNAME, "Comment"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Image not found with id:");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void deleteComment_ShouldDeleteComment_WhenUserIsOwner() {
        // Given
        Long commentId = 50L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(TEST_USER_ID);

        when(commentRepository.findByIdAndUserId(commentId, TEST_USER_ID)).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).delete(comment);

        // When
        imageService.deleteComment(commentId, TEST_USER_ID);

        // Then
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void deleteComment_ShouldThrowException_WhenCommentNotFoundOrForbidden() {
        // Given
        Long commentId = 50L;
        when(commentRepository.findByIdAndUserId(commentId, TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> imageService.deleteComment(commentId, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found with id:");

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void updateComment_ShouldUpdateText_WhenUserIsOwner() {
        // Given
        Long commentId = 50L;
        String newText = "Updated comment";
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(TEST_USER_ID);
        comment.setText("Old text");
        comment.setUsername(TEST_USERNAME);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());

        when(commentRepository.findByIdAndUserId(commentId, TEST_USER_ID)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // When
        CommentResponse response = imageService.updateComment(commentId, TEST_USER_ID, newText);

        // Then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository, times(1)).save(commentCaptor.capture());

        Comment updatedComment = commentCaptor.getValue();
        assertThat(updatedComment.getText()).isEqualTo(newText);
        assertThat(response.getText()).isEqualTo(newText);
    }

    @Test
    void updateComment_ShouldThrowException_WhenCommentNotFoundOrForbidden() {
        // Given
        Long commentId = 50L;
        when(commentRepository.findByIdAndUserId(commentId, TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> imageService.updateComment(commentId, TEST_USER_ID, "New text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found with id:");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    private Image createTestImage(Long id, Long userId) {
        Image image = new Image();
        image.setId(id);
        image.setUrl("test-key-" + id + "/image.jpg");
        image.setDescription("Test Image " + id);
        image.setUploadedAt(Instant.now());
        image.setUserId(userId);
        return image;
    }
}
