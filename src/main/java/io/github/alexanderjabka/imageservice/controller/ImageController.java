package io.github.alexanderjabka.imageservice.controller;

import com.amazonaws.services.s3.model.S3Object;
import io.github.alexanderjabka.imageservice.dto.CommentRequest;
import io.github.alexanderjabka.imageservice.dto.ImageMetadataResponse;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.service.ImageService;
import io.github.alexanderjabka.imageservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Validated
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final S3Service s3Service;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageMetadataResponse> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description,
            Authentication authentication
    ) throws IOException {
        Long userId = (Long) authentication.getPrincipal();
        if (description == null) description = "";
        ImageMetadataResponse response = imageService.uploadImage(file, description, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<ImageMetadataResponse> getImageById(@PathVariable Long id) {
        return ResponseEntity.ok(imageService.getImageMetadata(id));
    }

    @GetMapping("/images/{id}/content")
    public ResponseEntity<InputStreamResource> getImageContent(@PathVariable Long id) {
        String key = imageService.getS3Key(id);
        S3Object s3Object = s3Service.getObject(key);
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (s3Object.getObjectMetadata().getContentType() != null) {
            try {
                contentType = MediaType.parseMediaType(s3Object.getObjectMetadata().getContentType());
            } catch (Exception ignored) {
            }
        }
        InputStreamResource resource = new InputStreamResource(s3Object.getObjectContent());
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(resource);
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        imageService.deleteImage(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/user/images", "/images/user"})
    public ResponseEntity<PageResponse<ImageMetadataResponse>> getUserImages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        PageResponse<ImageMetadataResponse> response = imageService.getUserImagesWithPagination(
                userId, page, size, limit, sortBy, sortDirection
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/images")
    public ResponseEntity<PageResponse<ImageMetadataResponse>> getAllImages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageResponse<ImageMetadataResponse> response = imageService.getAllImagesWithPagination(
                page, size, limit, sortBy, sortDirection
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/images/{id}/likes")
    public ResponseEntity<Void> toggleLike(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        imageService.toggleLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/images/{id}/comments")
    public ResponseEntity<Void> addComment(@PathVariable Long id, @RequestBody CommentRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        imageService.addComment(id, userId, request.getText());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/images/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        imageService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/images/{id}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(@PathVariable Long id, @PathVariable Long commentId, @RequestBody CommentRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        imageService.updateComment(commentId, userId, request.getText());
        return ResponseEntity.ok().build();
    }
}
