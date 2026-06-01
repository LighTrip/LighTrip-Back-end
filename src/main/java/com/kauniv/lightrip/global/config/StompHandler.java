package com.kauniv.lightrip.global.config;

import com.kauniv.lightrip.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    // JwtProvider: validateToken(token), getUserId(token) 사용
    // WebSocketConfig에서 configureClientInboundChannel()로 등록됨
    private final JwtProvider jwtProvider;

    // CONNECT 시에만 JWT 검증. 이후 메시지는 세션에 저장된 userId를 사용하므로
    // 매 메시지마다 토큰을 다시 파싱하는 오버헤드 없음.
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessageDeliveryException("인증 토큰이 없습니다.");
            }
            String token = authHeader.substring(7);
            if (!jwtProvider.validateToken(token)) {
                throw new MessageDeliveryException("유효하지 않은 토큰입니다.");
            }
            Long userId = jwtProvider.getUserId(token);
            accessor.getSessionAttributes().put("userId", userId);
        }
        return message;
    }
}
