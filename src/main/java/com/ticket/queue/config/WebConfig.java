package com.ticket.queue.config;

import com.ticket.queue.interceptor.QueueInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final QueueInterceptor queueInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(queueInterceptor)
                .addPathPatterns("/reservations/**") // 예매 관련 API는 모두 이 문지기 거쳐야 함
                .excludePathPatterns("/queue/**"); // 대기열 진입/조회 API는 검증 제외

    }
}
