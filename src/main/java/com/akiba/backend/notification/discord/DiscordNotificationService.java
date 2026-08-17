package com.akiba.backend.notification.discord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Slf4j
@Service
public class DiscordNotificationService {

    @Value("${discord.webhook.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Discord 웹훅으로 메시지 전송
     * @param content 전송할 메시지 내용
     */
    public void send(String content) {
        try {
            restTemplate.postForObject(
                    webhookUrl,
                    Map.of("content", content),
                    String.class
            );
            log.info("Discord 알림 전송 완료");
        } catch (Exception e) {
            log.error("Discord 알림 전송 실패: {}", e.getMessage());
        }
    }
}