package com.nemall.support.controller;

import com.nemall.support.dto.ChatTurnDTO;
import com.nemall.support.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    // 智能导购入口
    @PostMapping("/guide/turns/stream")
    public SseEmitter streamGuide(
            @RequestHeader("user-info") Long userId,
            @RequestBody ChatTurnDTO request) {

        return supportService.streamGuide(userId, request.getText());
    }

    // 客服入口
    @PostMapping("/support/turns/stream")
    public SseEmitter streamSupport(
            @RequestHeader("user-info") Long userId,
            @RequestBody ChatTurnDTO request) {

        return supportService.streamSupport(userId, request.getText());
    }
}
