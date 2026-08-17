package com.akiba.backend.chat.dto;

import com.akiba.backend.chat.domain.ChatRoomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponse {
    private Long roomId;
    private ChatRoomType roomType;
    private Long marketPostId;
    private LocalDateTime createdAt;
}