package io.github.alexanderjabka.imageservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alexanderjabka.imageservice.AbstractIntegrationTest;
import io.github.alexanderjabka.imageservice.dto.CommentRequest;
import io.github.alexanderjabka.imageservice.dto.ImageMetadataResponse;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.entity.Comment;
import io.github.alexanderjabka.imageservice.entity.Image;
import io.github.alexanderjabka.imageservice.entity.Like;
import io.github.alexanderjabka.imageservice.repository.CommentRepository;
import io.github.alexanderjabka.imageservice.repository.ImageRepository;
import io.github.alexanderjabka.imageservice.repository.LikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ImageControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        likeRepository.deleteAll();
        imageRepository.deleteAll();

        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);
        }
    }

    @Test
    void uploadImage_ShouldUploadImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        MockMultipartFile description = new MockMultipartFile(
                "description",
                "",
                "text/plain",
                "Test Description".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/images")
                        .file(file)
                        .file(description)
                        .with(authentication(createAuthentication(TEST_USER_ID)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.contentUrl").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ImageMetadataResponse response = objectMapper.readValue(responseJson, ImageMetadataResponse.class);

        Image savedImage = imageRepository.findById(response.getId()).orElseThrow();
        assertThat(savedImage.getDescription()).isEqualTo("Test Description");
        assertThat(savedImage.getUserId()).isEqualTo(TEST_USER_ID);

        assertThat(s3Client.doesObjectExist(bucketName, savedImage.getUrl())).isTrue();
    }

    @Test
    void uploadImage_ShouldHandleEmptyDescription() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/images")
                        .file(file)
                        .with(authentication(createAuthentication(TEST_USER_ID)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(""));
    }

    @Test
    void getImageById_ShouldReturnImageMetadata_WhenImageExists() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Test Image");

        mockMvc.perform(get("/images/{id}", image.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(image.getId()))
                .andExpect(jsonPath("$.description").value("Test Image"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID));
    }

    @Test
    void getImageContent_ShouldReturnImageBytes_WhenImageExists() throws Exception {
        byte[] imageContent = "test image binary content".getBytes();
        Image image = createAndSaveImageWithS3Content(TEST_USER_ID, "Test", imageContent, "image/jpeg");

        MvcResult result = mockMvc.perform(get("/images/{id}/content", image.getId()))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andReturn();

        byte[] responseContent = result.getResponse().getContentAsByteArray();
        assertThat(responseContent).isEqualTo(imageContent);
    }

    @Test
    void deleteImage_ShouldDeleteImage_WhenUserIsOwner() throws Exception {
        byte[] content = "content".getBytes();
        Image image = createAndSaveImageWithS3Content(TEST_USER_ID, "To Delete", content, "image/jpeg");
        String s3Key = image.getUrl();

        mockMvc.perform(delete("/images/{id}", image.getId())
                        .with(authentication(createAuthentication(TEST_USER_ID))))
                .andExpect(status().isNoContent());

        assertThat(imageRepository.findById(image.getId())).isEmpty();
        assertThat(s3Client.doesObjectExist(bucketName, s3Key)).isFalse();
    }

    @Test
    void deleteImage_ShouldReturnError_WhenUserIsNotOwner() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Protected Image");

        mockMvc.perform(delete("/images/{id}", image.getId())
                        .with(authentication(createAuthentication(OTHER_USER_ID))))
                .andExpect(status().isForbidden());

        assertThat(imageRepository.findById(image.getId())).isPresent();
    }

    @Test
    void getUserImages_ShouldReturnOnlyUserImages() throws Exception {
        createAndSaveImage(TEST_USER_ID, "User 1 Image 1");
        createAndSaveImage(TEST_USER_ID, "User 1 Image 2");
        createAndSaveImage(OTHER_USER_ID, "User 2 Image");

        MvcResult result = mockMvc.perform(get("/user/images")
                        .param("page", "1")
                        .param("size", "10")
                        .with(authentication(createAuthentication(TEST_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        PageResponse<?> response = objectMapper.readValue(responseJson, PageResponse.class);
        assertThat(response.getTotalItems()).isEqualTo(2);
    }

    @Test
    void getAllImages_ShouldReturnAllImages() throws Exception {
        createAndSaveImage(TEST_USER_ID, "Image 1");
        createAndSaveImage(TEST_USER_ID, "Image 2");
        createAndSaveImage(OTHER_USER_ID, "Image 3");

        mockMvc.perform(get("/images")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void getAllImages_ShouldSupportPagination() throws Exception {
        for (int i = 0; i < 15; i++) {
            createAndSaveImage(TEST_USER_ID, "Image " + i);
        }

        mockMvc.perform(get("/images")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(15))
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/images")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5));
    }

    @Test
    void getAllImages_ShouldSupportSorting() throws Exception {
        createAndSaveImage(TEST_USER_ID, "Image A");
        Thread.sleep(100); // Ensure different timestamps
        createAndSaveImage(TEST_USER_ID, "Image B");

        mockMvc.perform(get("/images")
                        .param("sortBy", "uploadedAt")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].description").value("Image A"));

        mockMvc.perform(get("/images")
                        .param("sortBy", "uploadedAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].description").value("Image B"));
    }

    @Test
    void toggleLike_ShouldAddLike_WhenNoExistingLike() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Likeable Image");

        mockMvc.perform(post("/images/{id}/likes", image.getId())
                        .with(authentication(createAuthentication(OTHER_USER_ID))))
                .andExpect(status().isOk());

        assertThat(likeRepository.findByImageIdAndUserId(image.getId(), OTHER_USER_ID)).isPresent();
    }

    @Test
    void toggleLike_ShouldRemoveLike_WhenLikeExists() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Liked Image");
        Like like = new Like();
        like.setImageId(image.getId());
        like.setUserId(OTHER_USER_ID);
        like.setCreatedAt(Instant.now());
        likeRepository.save(like);

        mockMvc.perform(post("/images/{id}/likes", image.getId())
                        .with(authentication(createAuthentication(OTHER_USER_ID))))
                .andExpect(status().isOk());

        assertThat(likeRepository.findByImageIdAndUserId(image.getId(), OTHER_USER_ID)).isEmpty();
    }

    @Test
    void addComment_ShouldCreateComment_WhenValidDataProvided() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Commentable Image");
        CommentRequest request = new CommentRequest();
        request.setText("Great image!");

        mockMvc.perform(post("/images/{id}/comments", image.getId())
                        .with(authentication(createAuthentication(OTHER_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertThat(commentRepository.findByImageId(image.getId()))
                .hasSize(1)
                .first()
                .satisfies(comment -> {
                    assertThat(comment.getText()).isEqualTo("Great image!");
                    assertThat(comment.getUserId()).isEqualTo(OTHER_USER_ID);
                });
    }

    @Test
    void deleteComment_ShouldDeleteComment_WhenUserIsOwner() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Image");
        Comment comment = new Comment();
        comment.setImageId(image.getId());
        comment.setUserId(OTHER_USER_ID);
        comment.setText("My comment");
        comment.setCreatedAt(Instant.now());
        comment = commentRepository.save(comment);

        mockMvc.perform(delete("/images/{imageId}/comments/{commentId}", image.getId(), comment.getId())
                        .with(authentication(createAuthentication(OTHER_USER_ID))))
                .andExpect(status().isNoContent());

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    @Test
    void deleteComment_ShouldReturnError_WhenUserIsNotOwner() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Image");
        Comment comment = new Comment();
        comment.setImageId(image.getId());
        comment.setUserId(OTHER_USER_ID);
        comment.setText("Other user's comment");
        comment.setCreatedAt(Instant.now());
        comment = commentRepository.save(comment);

        mockMvc.perform(delete("/images/{imageId}/comments/{commentId}", image.getId(), comment.getId())
                        .with(authentication(createAuthentication(TEST_USER_ID))))
                .andExpect(status().isNotFound());

        assertThat(commentRepository.findById(comment.getId())).isPresent();
    }

    @Test
    void updateComment_ShouldUpdateText_WhenUserIsOwner() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Image");
        Comment comment = new Comment();
        comment.setImageId(image.getId());
        comment.setUserId(OTHER_USER_ID);
        comment.setText("Original text");
        comment.setCreatedAt(Instant.now());
        comment = commentRepository.save(comment);

        CommentRequest request = new CommentRequest();
        request.setText("Updated text");

        mockMvc.perform(put("/images/{imageId}/comments/{commentId}", image.getId(), comment.getId())
                        .with(authentication(createAuthentication(OTHER_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(updated.getText()).isEqualTo("Updated text");
    }

    @Test
    void updateComment_ShouldReturnError_WhenUserIsNotOwner() throws Exception {
        Image image = createAndSaveImage(TEST_USER_ID, "Image");
        Comment comment = new Comment();
        comment.setImageId(image.getId());
        comment.setUserId(OTHER_USER_ID);
        comment.setText("Original");
        comment.setCreatedAt(Instant.now());
        comment = commentRepository.save(comment);

        CommentRequest request = new CommentRequest();
        request.setText("Hacked text");

        mockMvc.perform(put("/images/{imageId}/comments/{commentId}", image.getId(), comment.getId())
                        .with(authentication(createAuthentication(TEST_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        Comment unchanged = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(unchanged.getText()).isEqualTo("Original");
    }

    private Image createAndSaveImage(Long userId, String description) {
        Image image = new Image();
        image.setUrl("test-key-" + System.currentTimeMillis() + "/image.jpg");
        image.setDescription(description);
        image.setUploadedAt(Instant.now());
        image.setUserId(userId);
        return imageRepository.save(image);
    }

    private Image createAndSaveImageWithS3Content(Long userId, String description, byte[] content, String contentType) {
        String key = "test-key-" + System.currentTimeMillis() + "/image.jpg";

        com.amazonaws.services.s3.model.ObjectMetadata metadata = new com.amazonaws.services.s3.model.ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(content.length);
        s3Client.putObject(bucketName, key, new java.io.ByteArrayInputStream(content), metadata);

        Image image = new Image();
        image.setUrl(key);
        image.setDescription(description);
        image.setUploadedAt(Instant.now());
        image.setUserId(userId);
        return imageRepository.save(image);
    }

    private org.springframework.security.core.Authentication createAuthentication(Long userId) {
        return new org.springframework.security.authentication.TestingAuthenticationToken(userId, null, "USER");
    }
}
