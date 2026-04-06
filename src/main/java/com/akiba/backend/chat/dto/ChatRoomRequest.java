package com.akiba.backend.chat.dto;

import com.akiba.backend.chat.domain.ChatRoomType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomRequest {
    private ChatRoomType roomType;
    private Long marketPostId;
    private Long targetUserId;
}