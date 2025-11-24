package io.github.alexanderjabka.imageservice.service;

import io.github.alexanderjabka.imageservice.dto.CommentResponse;
import io.github.alexanderjabka.imageservice.dto.ImageMetadataResponse;
import io.github.alexanderjabka.imageservice.dto.LikeToggleResponse;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.entity.Comment;
import io.github.alexanderjabka.imageservice.entity.Image;
import io.github.alexanderjabka.imageservice.entity.Like;
import io.github.alexanderjabka.imageservice.exception.CommentNotFoundException;
import io.github.alexanderjabka.imageservice.exception.ForbiddenException;
import io.github.alexanderjabka.imageservice.exception.ImageNotFoundException;
import io.github.alexanderjabka.imageservice.repository.CommentRepository;
import io.github.alexanderjabka.imageservice.repository.ImageRepository;
import io.github.alexanderjabka.imageservice.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final S3Service s3Service;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public ImageMetadataResponse uploadImage(MultipartFile file, String description, Long userId) throws IOException {
        String key = s3Service.upload(file);
        
        Image image = Image.builder()
                .url(key)
                .description(description)
                .uploadedAt(Instant.now())
                .userId(userId)
                .build();
        
        return toResponse(imageRepository.save(image), userId);
    }

    public Image getImageEntity(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new ImageNotFoundException(id));
    }

    public ImageMetadataResponse getImageMetadata(Long id, Long currentUserId) {
        Image image = getImageEntity(id);
        return toResponse(image, currentUserId);
    }

    public Page<ImageMetadataResponse> getAllImages(Pageable pageable, Long currentUserId) {
        Page<Image> page = imageRepository.findAll(pageable);
        List<ImageMetadataResponse> items = page.getContent().stream()
                .map(image -> toResponse(image, currentUserId))
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    public Page<ImageMetadataResponse> getUserImages(Long userId, Pageable pageable) {
        Page<Image> page = imageRepository.findByUserId(userId, pageable);
        List<ImageMetadataResponse> items = page.getContent().stream()
                .map(image -> toResponse(image, userId))
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    public String getS3Key(Long id) {
        return getImageEntity(id).getUrl();
    }

    public void deleteImage(Long id, Long userId) {
        Image image = getImageEntity(id);
        if (!image.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own images");
        }
        s3Service.delete(image.getUrl());
        imageRepository.deleteById(id);
    }

    private ImageMetadataResponse toResponse(Image image, Long currentUserId) {
        String contentUrl = "/images/" + image.getId() + "/content";
        long likesCount = likeRepository.countByImageId(image.getId());
        boolean likedByCurrentUser = likeRepository.existsByImageIdAndUserId(image.getId(), currentUserId);
        long commentsCount = commentRepository.countByImageId(image.getId());
        return ImageMetadataResponse.builder()
                .id(image.getId())
                .description(image.getDescription())
                .uploadedAt(image.getUploadedAt())
                .userId(image.getUserId())
                .contentUrl(contentUrl)
                .likesCount(likesCount)
                .likedByCurrentUser(likedByCurrentUser)
                .commentsCount(commentsCount)
                .build();
    }

    public PageResponse<ImageMetadataResponse> getUserImagesWithPagination(Long userId, int page, Integer size, Integer limit, String sortBy, String sortDirection) {
        int pageSize = (size != null) ? size : (limit != null) ? limit : 12;
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(direction, sortBy));
        Page<ImageMetadataResponse> result = getUserImages(userId, pageable);
        return new PageResponse<>(
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber() + 1,
                result.getSize(),
                result.getContent()
        );
    }

    public PageResponse<ImageMetadataResponse> getAllImagesWithPagination(Long currentUserId, int page, Integer size, Integer limit, String sortBy, String sortDirection) {
        int pageSize = (size != null) ? size : (limit != null) ? limit : 12;
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(direction, sortBy));
        Page<ImageMetadataResponse> result = getAllImages(pageable, currentUserId);
        return new PageResponse<>(
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber() + 1,
                result.getSize(),
                result.getContent()
        );
    }

    public LikeToggleResponse toggleLike(Long imageId, Long userId) {
        getImageEntity(imageId);
        boolean liked;
        var existing = likeRepository.findByImageIdAndUserId(imageId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(Like.builder()
                    .imageId(imageId)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .build());
            liked = true;
        }

        long likesCount = likeRepository.countByImageId(imageId);
        return LikeToggleResponse.builder()
                .imageId(imageId)
                .liked(liked)
                .likesCount(likesCount)
                .build();
    }

    public CommentResponse addComment(Long imageId, Long userId, String username, String text) {
        getImageEntity(imageId);
        Instant now = Instant.now();
        String effectiveUsername = (username != null && !username.isBlank()) ? username : "user-" + userId;
        Comment comment = commentRepository.save(Comment.builder()
                .imageId(imageId)
                .userId(userId)
                .username(effectiveUsername)
                .text(text)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toCommentResponse(comment);
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        commentRepository.delete(comment);
    }

    public CommentResponse updateComment(Long commentId, Long userId, String text) {
        Comment comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        comment.setText(text);
        comment.setUpdatedAt(Instant.now());
        commentRepository.save(comment);
        return toCommentResponse(comment);
    }

    public List<CommentResponse> getComments(Long imageId) {
        getImageEntity(imageId);
        return commentRepository.findByImageId(imageId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .imageId(comment.getImageId())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
