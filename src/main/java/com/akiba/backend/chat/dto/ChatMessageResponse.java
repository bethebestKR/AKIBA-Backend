package com.akiba.backend.chat.dto;

import com.akiba.backend.chat.domain.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private MessageType messageType;
    private String content;
    private Long mediaId;
    private LocalDateTime createdAt;
}