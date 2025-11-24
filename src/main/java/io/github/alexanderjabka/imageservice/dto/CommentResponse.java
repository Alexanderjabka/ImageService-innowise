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
public class CommentResponse {
    private Long id;
    private Long imageId;
    private Long userId;
    private String username;
    private String text;
    private Instant createdAt;
    private Instant updatedAt;
}


