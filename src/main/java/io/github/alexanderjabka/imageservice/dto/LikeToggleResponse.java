package io.github.alexanderjabka.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeToggleResponse {
    private Long imageId;
    private boolean liked;
    private long likesCount;
}


