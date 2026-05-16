package com.kauniv.lightrip.domain.team.controller;

import com.kauniv.lightrip.domain.team.dto.request.LocationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MapSyncController {

    @MessageMapping("/team/{teamId}/location")
    @SendTo("/topic/team/{teamId}")
    public LocationMessage syncLocation(
            @DestinationVariable Long teamId,
            LocationMessage message
    ) {
        return message;
    }
}