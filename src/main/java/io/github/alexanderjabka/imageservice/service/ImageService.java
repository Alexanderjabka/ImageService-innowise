package io.github.alexanderjabka.imageservice.service;

import io.github.alexanderjabka.imageservice.dto.ImageMetadataResponse;
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
        
        return toResponse(imageRepository.save(image));
    }

    public Image getImageEntity(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new ImageNotFoundException(id));
    }

    public ImageMetadataResponse getImageMetadata(Long id) {
        Image image = getImageEntity(id);
        return toResponse(image);
    }

    public Page<ImageMetadataResponse> getAllImages(Pageable pageable) {
        Page<Image> page = imageRepository.findAll(pageable);
        List<ImageMetadataResponse> items = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    public Page<ImageMetadataResponse> getUserImages(Long userId, Pageable pageable) {
        Page<Image> page = imageRepository.findByUserId(userId, pageable);
        List<ImageMetadataResponse> items = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
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

    private ImageMetadataResponse toResponse(Image image) {
        String contentUrl = "/images/" + image.getId() + "/content";
        return new ImageMetadataResponse(image.getId(), image.getDescription(), image.getUploadedAt(), image.getUserId(), contentUrl);
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

    public PageResponse<ImageMetadataResponse> getAllImagesWithPagination(int page, Integer size, Integer limit, String sortBy, String sortDirection) {
        int pageSize = (size != null) ? size : (limit != null) ? limit : 12;
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by(direction, sortBy));
        Page<ImageMetadataResponse> result = getAllImages(pageable);
        return new PageResponse<>(
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber() + 1,
                result.getSize(),
                result.getContent()
        );
    }

    public void toggleLike(Long imageId, Long userId) {
        getImageEntity(imageId);
        likeRepository.findByImageIdAndUserId(imageId, userId)
                .ifPresentOrElse(
                        likeRepository::delete,
                        () -> likeRepository.save(Like.builder()
                                .imageId(imageId)
                                .userId(userId)
                                .createdAt(Instant.now())
                                .build())
                );
    }

    public void addComment(Long imageId, Long userId, String text) {
        getImageEntity(imageId);
        commentRepository.save(Comment.builder()
                .imageId(imageId)
                .userId(userId)
                .text(text)
                .createdAt(Instant.now())
                .build());
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        commentRepository.delete(comment);
    }

    public void updateComment(Long commentId, Long userId, String text) {
        Comment comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        comment.setText(text);
        commentRepository.save(comment);
    }
}
