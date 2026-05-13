package com.akiba.backend.chat.dto;

import com.akiba.backend.chat.domain.ChatRoomType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomRequest {

    @NotNull(message = "roomType 은 필수입니다.")
    private ChatRoomType roomType;

    private Long marketPostId;

    @NotNull(message = "targetUserId 는 필수입니다.")
    private Long targetUserId;
}