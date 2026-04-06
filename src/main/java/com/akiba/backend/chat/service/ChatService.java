package com.akiba.backend.chat.service;

import com.akiba.backend.chat.domain.ChatMessage;
import com.akiba.backend.chat.domain.ChatRoom;
import com.akiba.backend.chat.domain.ChatRoomMember;
import com.akiba.backend.chat.dto.*;
import com.akiba.backend.chat.repository.ChatMessageRepository;
import com.akiba.backend.chat.repository.ChatRoomMemberRepository;
import com.akiba.backend.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 채팅방 생성
    public ChatRoomResponse createRoom(Long userId, ChatRoomRequest request) {
        // 마켓 채팅이면 기존 채팅방 있는지 확인
        if (request.getMarketPostId() != null) {
            ChatRoom existing = chatRoomRepository.findByMarketPostIdAndBuyerId(request.getMarketPostId(), userId)
                    .orElse(null);
            if (existing != null) {
                return ChatRoomResponse.builder()
                        .roomId(existing.getRoomId())
                        .roomType(existing.getRoomType())
                        .marketPostId(existing.getMarketPostId())
                        .createdAt(existing.getCreatedAt())
                        .build();
            }
        }

        ChatRoom room = ChatRoom.builder()
                .roomType(request.getRoomType())
                .marketPostId(request.getMarketPostId())
                .buyerId(userId)
                .build();
        chatRoomRepository.save(room);

        // 채팅방 생성자 멤버 추가
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .roomId(room.getRoomId())
                .userId(userId)
                .build());

        // 상대방 멤버 추가
        if (request.getTargetUserId() != null) {
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .roomId(room.getRoomId())
                    .userId(request.getTargetUserId())
                    .build());
        }

        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomType(room.getRoomType())
                .marketPostId(room.getMarketPostId())
                .createdAt(room.getCreatedAt())
                .build();
    }

    // 채팅방 나가기 (삭제)
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        // 나만 멤버에서 삭제
        chatRoomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);

        // 멤버가 아무도 없으면 채팅방이랑 메시지도 삭제
        if (chatRoomMemberRepository.findByRoomId(roomId).isEmpty()) {
            chatMessageRepository.deleteAll(chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId));
            chatRoomRepository.deleteById(roomId);
        }
    }

    // 내 채팅방 목록
    public List<ChatRoomResponse> getMyRooms(Long userId) {
        return chatRoomMemberRepository.findByUserId(userId).stream()
                .map(member -> chatRoomRepository.findById(member.getRoomId())
                        .map(room -> ChatRoomResponse.builder()
                                .roomId(room.getRoomId())
                                .roomType(room.getRoomType())
                                .marketPostId(room.getMarketPostId())
                                .createdAt(room.getCreatedAt())
                                .build())
                        .orElse(null))
                .collect(Collectors.toList());
    }

    // 메시지 저장 및 반환
    public ChatMessageResponse saveMessage(ChatMessageRequest request, Long senderId) {
        ChatMessage message = ChatMessage.builder()
                .roomId(request.getRoomId())
                .senderId(senderId)
                .messageType(request.getMessageType())
                .content(request.getContent())
                .mediaId(request.getMediaId())
                .build();
        chatMessageRepository.save(message);

        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .mediaId(message.getMediaId())
                .createdAt(message.getCreatedAt())
                .build();
    }

    // 채팅방 메시지 목록
    public List<ChatMessageResponse> getMessages(Long roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(message -> ChatMessageResponse.builder()
                        .messageId(message.getMessageId())
                        .roomId(message.getRoomId())
                        .senderId(message.getSenderId())
                        .messageType(message.getMessageType())
                        .content(message.getContent())
                        .mediaId(message.getMediaId())
                        .createdAt(message.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}