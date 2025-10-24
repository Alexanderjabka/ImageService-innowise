package io.github.alexanderjabka.imageservice.service;

import io.github.alexanderjabka.imageservice.dto.LikeResponse;
import io.github.alexanderjabka.imageservice.entity.Like;
import io.github.alexanderjabka.imageservice.exception.ImageNotFoundException;
import io.github.alexanderjabka.imageservice.repository.ImageRepository;
import io.github.alexanderjabka.imageservice.repository.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class LikeService {
    
    @Autowired
    private LikeRepository likeRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    public LikeResponse toggleLike(Long imageId, Long userId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ImageNotFoundException("Image not found with id: " + imageId);
        }
        
        Optional<Like> existingLike = likeRepository.findByImageIdAndUserId(imageId, userId);
        
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return new LikeResponse(null, imageId, userId, null, false);
        } else {
            Like newLike = new Like(imageId, userId);
            Like savedLike = likeRepository.save(newLike);
            return new LikeResponse(savedLike.getId(), imageId, userId, savedLike.getCreatedAt(), true);
        }
    }
    
    @Transactional(readOnly = true)
    public LikeResponse getLikeStatus(Long imageId, Long userId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ImageNotFoundException("Image not found with id: " + imageId);
        }
        
        Optional<Like> like = likeRepository.findByImageIdAndUserId(imageId, userId);
        
        if (like.isPresent()) {
            return new LikeResponse(like.get().getId(), imageId, userId, like.get().getCreatedAt(), true);
        } else {
            return new LikeResponse(null, imageId, userId, null, false);
        }
    }
}
