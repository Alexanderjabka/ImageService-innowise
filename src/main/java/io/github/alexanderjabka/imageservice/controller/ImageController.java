package io.github.alexanderjabka.imageservice.controller;

import io.github.alexanderjabka.imageservice.dto.ImageResponse;
import io.github.alexanderjabka.imageservice.dto.ImageUploadRequest;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.security.JwtUtil;
import io.github.alexanderjabka.imageservice.service.ImageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    
    @Autowired
    private ImageService imageService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping
    public ResponseEntity<ImageResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) throws IOException {
        
        Long userId = jwtUtil.extractUserId(httpRequest);
        ImageUploadRequest request = new ImageUploadRequest(description);
        ImageResponse response = imageService.uploadImage(file, request, userId);
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getImageById(@PathVariable Long id) {
        ImageResponse response = imageService.getImageById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @GetMapping
    public ResponseEntity<PageResponse<ImageResponse>> getAllImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PageResponse<ImageResponse> response = imageService.getAllImages(pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<ImageResponse>> getUserImages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PageResponse<ImageResponse> response = imageService.getUserImages(userId, pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = jwtUtil.extractUserId(httpRequest);
        imageService.deleteImage(id, userId);
        
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

