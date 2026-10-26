package com.nemall.support.service.impl;

import com.nemall.support.client.AgentClient;
import com.nemall.support.dto.AgentEventDTO;
import com.nemall.support.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        } else if (!"done".equals(event.getType())) {
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
