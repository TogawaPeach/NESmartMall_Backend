package com.nemall.support.service;

import com.nemall.support.dto.HandoffResultDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SupportService {

    SseEmitter streamGuide(Long userId, String text, String authorization);

    SseEmitter streamSupport(Long userId, String text, String authorization);

    HandoffResultDTO createSupportTicket(Long userId, String actionId, String authorization);
}
