package com.ticket.reservation.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {
    @Bean // 에러 발생 메시지를 즉시 원본토픽명 + .dlq로 포워딩하는 복구 장치
    public DeadLetterPublishingRecoverer recoverer(KafkaTemplate<Object, Object> template) {
        return new DeadLetterPublishingRecoverer(template);
    }

    @Bean // 대용량 피크 트래픽 맞춤형 에러 핸들러 (순서 보장형)
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
        // 대기시간 500ms, 최대 시도 3회 (multiplier x)
        // 총 4회 시도
        FixedBackOff fixedBackOff = new FixedBackOff(500L, 3L);
        return new DefaultErrorHandler(recoverer, fixedBackOff);
    }

    @Bean // 위 에러 핸들러를 컨슈머 팩토리에 장착
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 전역 에러 핸들러 주입 => 모든 @KafkaListener가 이 규칙을 따름
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
