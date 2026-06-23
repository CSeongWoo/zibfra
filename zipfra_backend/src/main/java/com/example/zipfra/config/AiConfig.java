package com.example.zipfra.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient Bean 설정 (AGENTS.md §3 확정 스택).
 *
 * <p>application.yml의 spring.ai.openai.* 설정을 자동 주입한다.
 * ChatClient.Builder는 spring-ai-openai-spring-boot-starter가 자동 구성한다.
 *
 * <p><b>주의</b>: RestTemplate 직접 LLM 호출 금지 (AGENTS.md §3·§6.2).
 * 모든 LLM 요청은 이 ChatClient를 통해서만 이루어진다.
 */
@Configuration
public class AiConfig {

    /**
     * ChatClient 빈 등록.
     *
     * <ul>
     *   <li>모델: gpt-4o-mini (application.yml spring.ai.openai.chat.options.model)</li>
     *   <li>타임아웃: 10s (spring.ai.openai.chat.options.timeout)</li>
     *   <li>온도: 0.3 (사실 기반 요약, 낮은 창의성)</li>
     * </ul>
     *
     * @param builder Spring AI 자동 구성 빌더
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(
                    // 기본 시스템 가드레일 (각 Planner가 오버라이드 가능)
                    "당신은 한국 부동산 정보를 객관적으로 요약하는 AI입니다. " +
                    "가격 예측, 투자 추천, 법률 조언은 절대 하지 마십시오."
                )
                .build();
    }
}
