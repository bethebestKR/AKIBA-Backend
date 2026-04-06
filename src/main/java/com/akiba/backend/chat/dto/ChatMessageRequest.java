package com.akiba.backend.chat.dto;

import com.akiba.backend.chat.domain.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    private Long roomId;
    private MessageType messageType;
    private String content;
    private Long mediaId;
}