package com.akiba.backend.chat.controller;

import com.akiba.backend.chat.dto.*;
import com.akiba.backend.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // 채팅방 생성
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @AuthenticationPrincipal Long userId,
            @RequestBody ChatRoomRequest request) {
        return ResponseEntity.ok(chatService.createRoom(userId, request));
    }

    // 내 채팅방 목록
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(chatService.getMyRooms(userId));
    }

    // 채팅방 나가기
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> leaveRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId) {
        chatService.leaveRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    // 채팅방 메시지 목록
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getMessages(roomId));
    }

    // 웹소켓 메시지 전송
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        ChatMessageResponse response = chatService.saveMessage(request, userId);
        messagingTemplate.convertAndSend("/topic/chat/" + request.getRoomId(), response);
    }
}