package com.nemall.support.service.impl;

import com.nemall.support.client.AgentClient;
import com.nemall.support.dto.AgentEventDTO;
import com.nemall.support.dto.HandoffResultDTO;
import com.nemall.support.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    private final AgentClient agentClient;

    @Override
    public SseEmitter streamGuide(Long userId, String text, String authorization) {
        return forward(agentClient.streamGuide(userId, text, authorization));
    }

    @Override
    public SseEmitter streamSupport(Long userId, String text, String authorization) {
        return forward(agentClient.streamSupport(userId, text, authorization));
    }

    @Override
    public HandoffResultDTO createSupportTicket(
            Long userId, String actionId, String authorization) {
        try {
            HandoffResultDTO result = agentClient
                    .createSupportTicket(userId, actionId, authorization)
                    .block(Duration.ofSeconds(120));
            if (result == null) {
                throw new IllegalStateException("Python 未返回工单结果");
            }
            return result;
        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode().value() == 410) {
                throw new ResponseStatusException(
                        HttpStatus.GONE, "申请入口已过期，请重新提出人工服务请求。"
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "提交失败，请稍后重试。"
            );
        } catch (RuntimeException exception) {
            log.warn("人工服务申请提交失败：{}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "提交失败，请稍后重试。"
            );
        }
    }

    private SseEmitter forward(Flux<AgentEventDTO> events) {
        SseEmitter emitter = new SseEmitter(180_000L);

        Disposable.Swap subscription = Disposables.swap();
        AtomicBoolean receivedDone = new AtomicBoolean(false);

        // 前端连接结束时，取消对应的 Python 请求。
        emitter.onCompletion(subscription::dispose);
        emitter.onError(error -> subscription.dispose());
        emitter.onTimeout(() -> {
            subscription.dispose();
            fail(emitter, new TimeoutException("回答超过整轮时间限制"));
        });

        subscription.update(
                events
                        .timeout(Duration.ofSeconds(60))
                        // 向前端写出可能阻塞，放到工作线程处理。
                        .publishOn(Schedulers.boundedElastic(), 1)
                        .takeUntil(event -> "done".equals(event.getType()))
                        .subscribe(
                                event -> {
                                    sendEvent(emitter, event);

                                    if ("done".equals(event.getType())) {
                                        receivedDone.set(true);
                                    }
                                },
                                error -> fail(emitter, error),
                                () -> {
                                    if (receivedDone.get()) {
                                        emitter.complete();
                                    } else {
                                        fail(emitter, new IllegalStateException(
                                                "Python 连接结束，但未收到 done"
                                        ));
                                    }
                                }
                        )
        );

        return emitter;
    }

    private void sendEvent(
            SseEmitter emitter,
            AgentEventDTO event) {

        if ("delta".equals(event.getType())) {
            if (event.getPayload() == null
                    || event.getPayload().getText() == null) {
                throw new IllegalStateException("Python delta 缺少文本");
            }
        } else if (!"done".equals(event.getType())
                && !"action".equals(event.getType())) {
            throw new IllegalStateException("Python 返回异常事件");
        }

        try {
            emitter.send(event);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void fail(SseEmitter emitter, Throwable error) {
        // 写出失败通常表示前端已断开，由容器处理连接结束。
        if (error instanceof UncheckedIOException) {
            return;
        }

        log.warn("Python Agent 回答流中断", error);

        try {
            emitter.send(Map.of(
                    "type", "error",
                    "payload", Map.of()
            ));
            emitter.complete();
        } catch (IOException | IllegalStateException ignored) {
            // 连接已不可写，不再发送事件。
        }
    }
}
