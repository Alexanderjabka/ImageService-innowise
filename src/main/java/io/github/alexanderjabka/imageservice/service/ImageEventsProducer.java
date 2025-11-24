package io.github.alexanderjabka.imageservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ImageEventsProducer {

    private static final String LIKES_TOPIC = "likes-events";
    private static final String COMMENTS_TOPIC = "comments-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendAddLike(Long imageId, Long userId) {
        sendLikeEvent("add_like", imageId, userId);
    }

    public void sendRemoveLike(Long imageId, Long userId) {
        sendLikeEvent("remove_like", imageId, userId);
    }

    public void sendCreateComment(Long imageId, Long userId, Long commentId) {
        sendCommentEvent("create_comment", imageId, userId, commentId);
    }

    public void sendRemoveComment(Long imageId, Long userId, Long commentId) {
        sendCommentEvent("remove_comment", imageId, userId, commentId);
    }

    private void sendLikeEvent(String type, Long imageId, Long userId) {
        String payload = String.format(
                "{\"type\":\"%s\",\"imageId\":%d,\"userId\":%d,\"timestamp\":\"%s\"}",
                type,
                imageId,
                userId,
                Instant.now().toString()
        );
        kafkaTemplate.send(LIKES_TOPIC, imageId.toString(), payload);
    }

    private void sendCommentEvent(String type, Long imageId, Long userId, Long commentId) {
        String payload = String.format(
                "{\"type\":\"%s\",\"imageId\":%d,\"userId\":%d,\"commentId\":%d,\"timestamp\":\"%s\"}",
                type,
                imageId,
                userId,
                commentId,
                Instant.now().toString()
        );
        kafkaTemplate.send(COMMENTS_TOPIC, imageId.toString(), payload);
    }
}
