package com.nemall.support.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SupportService {

    SseEmitter streamGuide(Long userId, String text, String authorization);

    SseEmitter streamSupport(Long userId, String text, String authorization);
}
