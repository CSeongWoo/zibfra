package com.example.zipfra.service.ai;

import com.example.zipfra.domain.AiSummary;
import com.example.zipfra.dto.ai.AiSummaryRequest;
import com.example.zipfra.dto.ai.AiSummaryResponse;
import com.example.zipfra.mapper.mysql.AiSummaryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSummaryServiceTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private AiSummaryMapper aiSummaryMapper;
    @Mock
    private AiSummaryTools aiSummaryTools;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @InjectMocks
    private AiSummaryService aiSummaryService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReflectionTestUtils.setField(aiSummaryService, "dailyLimit", 100);
        ReflectionTestUtils.setField(aiSummaryService, "cacheTtlHours", 24);

        ConcurrentHashMap<Long, AtomicInteger> quotaMap = (ConcurrentHashMap<Long, AtomicInteger>) ReflectionTestUtils
                .getField(aiSummaryService, "quotaMap");
        if (quotaMap != null) {
            quotaMap.clear();
        }
    }

    @Test
    @DisplayName("1. Cache Hit 시나리오: DB 캐시가 존재하면 LLM 호출 없이 Early Return 한다")
    void cacheHitScenario() throws Exception {
        // given
        Long userId = 1L;
        Long propertyId = 100L;
        AiSummaryRequest request = new AiSummaryRequest();
        ReflectionTestUtils.setField(request, "summaryType", AiSummaryRequest.SummaryType.PROPERTY);
        ReflectionTestUtils.setField(request, "propertyId", propertyId);

        String cachedJson = "{\"summaryAvailable\":true,\"propertyId\":100,\"summary\":\"캐시된 요약입니다.\"}";
        AiSummary cachedSummary = AiSummary.builder()
                .id(1L)
                .summaryType("PROPERTY")
                .targetId(propertyId)
                .summaryAvailable(true)
                .summaryJson(cachedJson)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        AiSummaryResponse expectedResponse = AiSummaryResponse.builder()
                .summaryAvailable(true)
                .propertyId(propertyId)
                .summary("캐시된 요약입니다.")
                .build();

        when(aiSummaryMapper.findValid("PROPERTY", propertyId, null)).thenReturn(cachedSummary);
        when(objectMapper.readValue(cachedJson, AiSummaryResponse.class)).thenReturn(expectedResponse);

        // when
        AiSummaryResponse response = aiSummaryService.summarize(userId, request);

        // then
        assertTrue(response.isSummaryAvailable());
        assertEquals("캐시된 요약입니다.", response.getSummary());

        verifyNoInteractions(chatClient);
        verify(aiSummaryMapper, never()).upsert(any(), any(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("2. Cache Miss & LLM 연쇄 호출 시나리오: 캐시가 없으면 LLM을 호출하고 Upsert 한다")
    void cacheMissAndLlmCallScenario() throws Exception {
        // given
        Long userId = 2L;
        Long propertyId = 200L;
        AiSummaryRequest request = new AiSummaryRequest();
        ReflectionTestUtils.setField(request, "summaryType", AiSummaryRequest.SummaryType.PROPERTY);
        ReflectionTestUtils.setField(request, "propertyId", propertyId);

        when(aiSummaryMapper.findValid("PROPERTY", propertyId, null)).thenReturn(null);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("LLM이 생성한 새로운 요약입니다.");

        when(objectMapper.writeValueAsString(any(AiSummaryResponse.class))).thenReturn("{\"dummy\":\"json\"}");

        // when
        AiSummaryResponse response = aiSummaryService.summarize(userId, request);

        // then
        assertTrue(response.isSummaryAvailable());
        assertEquals("LLM이 생성한 새로운 요약입니다.", response.getSummary());

        verify(chatClient, times(1)).prompt();
        verify(aiSummaryMapper, times(1)).upsert(
                eq("PROPERTY"),
                eq(propertyId),
                isNull(),
                anyString(),
                eq(24));
    }

    @Test
    @DisplayName("3. LLM Timeout 및 Fallback 격리 테스트: LLM 예외 발생 시 캐시에 저장하지 않는다")
    void llmTimeoutFallbackIsolationTest() {
        // given
        Long userId = 3L;
        Long propertyId = 300L;
        AiSummaryRequest request = new AiSummaryRequest();
        ReflectionTestUtils.setField(request, "summaryType", AiSummaryRequest.SummaryType.PROPERTY);
        ReflectionTestUtils.setField(request, "propertyId", propertyId);

        when(aiSummaryMapper.findValid("PROPERTY", propertyId, null)).thenReturn(null);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("LLM Timeout Exception"));

        // when
        AiSummaryResponse response = aiSummaryService.summarize(userId, request);

        // then
        assertFalse(response.isSummaryAvailable());
        assertEquals(propertyId, response.getPropertyId());
        assertNull(response.getSummary());

        verify(aiSummaryMapper, never()).upsert(any(), any(), any(), anyString(), anyInt());
    }
}
