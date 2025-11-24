package io.github.alexanderjabka.imageservice.controller;

import com.amazonaws.services.s3.model.S3Object;
import io.github.alexanderjabka.imageservice.dto.CommentRequest;
import io.github.alexanderjabka.imageservice.dto.CommentResponse;
import io.github.alexanderjabka.imageservice.dto.ImageMetadataResponse;
import io.github.alexanderjabka.imageservice.dto.LikeToggleResponse;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.security.UserPrincipal;
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

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import java.util.List;
import java.util.Objects;

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
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String imageDescription = description != null ? description : "";
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(imageService.uploadImage(file, imageDescription, principal.id()));
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<ImageMetadataResponse> getImageById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(imageService.getImageMetadata(id, principal.id()));
    }

    @GetMapping("/images/{id}/content")
    public ResponseEntity<InputStreamResource> getImageContent(@PathVariable Long id) {
        S3Object s3Object = s3Service.getObject(imageService.getS3Key(id));
        
        MediaType contentType = Optional.ofNullable(s3Object.getObjectMetadata().getContentType())
                .map(type -> {
                    try {
                        return MediaType.parseMediaType(type);
                    } catch (Exception e) {
                        return MediaType.APPLICATION_OCTET_STREAM;
                    }
                })
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(new InputStreamResource(s3Object.getObjectContent()));
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        imageService.deleteImage(id, principal.id());
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
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(imageService.getUserImagesWithPagination(
                principal.id(), page, size, limit, sortBy, sortDirection));
    }

    @GetMapping("/images")
    public ResponseEntity<PageResponse<ImageMetadataResponse>> getAllImages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(imageService.getAllImagesWithPagination(
                principal.id(), page, size, limit, sortBy, sortDirection));
    }

    @PostMapping("/images/{id}/likes")
    public ResponseEntity<LikeToggleResponse> toggleLike(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(imageService.toggleLike(id, principal.id()));
    }

    @PostMapping("/images/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @RequestBody @Valid CommentRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(imageService.addComment(id, principal.id(), principal.username(), request.getText()));
    }

    @DeleteMapping("/images/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        imageService.deleteComment(commentId, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/images/{id}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(
                imageService.updateComment(commentId, principal.id(), request.getText())
        );
    }

    @GetMapping("/images/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long id, Authentication authentication) {
        Objects.requireNonNull(authentication, "Authentication is required");
        return ResponseEntity.ok(imageService.getComments(id));
    }
}
