package com.akiba.backend.chat.repository;

import com.akiba.backend.chat.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    List<ChatRoomMember> findByUserId(Long userId);
    List<ChatRoomMember> findByRoomId(Long roomId);
    void deleteByRoomId(Long roomId);
    void deleteByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
