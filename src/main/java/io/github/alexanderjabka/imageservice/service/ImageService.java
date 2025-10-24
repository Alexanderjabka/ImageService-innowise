package io.github.alexanderjabka.imageservice.service;

import io.github.alexanderjabka.imageservice.dto.ImageResponse;
import io.github.alexanderjabka.imageservice.dto.ImageUploadRequest;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.entity.Image;
import io.github.alexanderjabka.imageservice.exception.ImageNotFoundException;
import io.github.alexanderjabka.imageservice.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ImageService {
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private S3Service s3Service;
    
    public ImageResponse uploadImage(MultipartFile file, ImageUploadRequest request, Long userId) throws IOException {
        String s3Url = s3Service.uploadFile(file, userId);
        Image image = new Image(s3Url, request.getDescription(), userId);
        Image savedImage = imageRepository.save(image);
        
        return convertToResponse(savedImage);
    }
    
    @Transactional(readOnly = true)
    public ImageResponse getImageById(Long id) {
        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new ImageNotFoundException("Image not found with id: " + id));
        
        return convertToResponse(image);
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ImageResponse> getUserImages(Long userId, Pageable pageable) {
        Page<Image> imagePage = imageRepository.findByUserId(userId, pageable);
        return convertToPageResponse(imagePage);
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ImageResponse> getAllImages(Pageable pageable) {
        Page<Image> imagePage = imageRepository.findAllByOrderByUploadedAtDesc(pageable);
        return convertToPageResponse(imagePage);
    }
    
    public void deleteImage(Long id, Long userId) {
        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new ImageNotFoundException("Image not found with id: " + id));
        
        if (!image.getUserId().equals(userId)) {
            throw new ImageNotFoundException("You don't have permission to delete this image");
        }
        
        s3Service.deleteFile(image.getUrl());
        imageRepository.delete(image);
    }
    
    private ImageResponse convertToResponse(Image image) {
        return new ImageResponse(
            image.getId(),
            image.getUrl(),
            image.getDescription(),
            image.getUserId(),
            image.getUploadedAt(),
            image.getLikesCount(),
            image.getCommentsCount()
        );
    }
    
    private PageResponse<ImageResponse> convertToPageResponse(Page<Image> imagePage) {
        List<ImageResponse> content = imagePage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return new PageResponse<>(
            content,
            imagePage.getNumber(),
            imagePage.getSize(),
            imagePage.getTotalElements(),
            imagePage.getTotalPages(),
            imagePage.isFirst(),
            imagePage.isLast(),
            imagePage.hasNext(),
            imagePage.hasPrevious()
        );
    }
}
