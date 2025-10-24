package io.github.alexanderjabka.imageservice.controller;

import io.github.alexanderjabka.imageservice.dto.LikeResponse;
import io.github.alexanderjabka.imageservice.security.JwtUtil;
import io.github.alexanderjabka.imageservice.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
public class LikeController {
    
    @Autowired
    private LikeService likeService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/{imageId}/likes")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable Long imageId,
            HttpServletRequest httpRequest) {
        
        Long userId = jwtUtil.extractUserId(httpRequest);
        LikeResponse response = likeService.toggleLike(imageId, userId);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @GetMapping("/{imageId}/likes")
    public ResponseEntity<LikeResponse> getLikeStatus(
            @PathVariable Long imageId,
            HttpServletRequest httpRequest) {
        
        Long userId = jwtUtil.extractUserId(httpRequest);
        LikeResponse response = likeService.getLikeStatus(imageId, userId);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
