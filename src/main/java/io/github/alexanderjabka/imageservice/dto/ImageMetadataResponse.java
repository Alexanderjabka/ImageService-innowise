package io.github.alexanderjabka.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageMetadataResponse {
    private Long id;
    private String description;
    private Instant uploadedAt;
    private Long userId;
    private String contentUrl;
    private long likesCount;
    private boolean likedByCurrentUser;
    private long commentsCount;
}
