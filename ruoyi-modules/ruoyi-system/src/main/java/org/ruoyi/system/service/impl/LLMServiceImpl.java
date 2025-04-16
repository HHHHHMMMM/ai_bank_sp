package org.ruoyi.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.chat.config.ChatConfig;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.knowledgegraph.service.impl.LLMService;
import org.ruoyi.system.domain.SysModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.ruoyi.common.chat.entity.chat.*;


@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);
    private OpenAiStreamClient openAiStreamClient;
    private final ChatConfig chatConfig;


    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String askLLM(String systemPrompt, String userPrompt, String modelName, Map<String, Object> params) {
        try {
            // 从ModelService获取模型信息
            SysModel sysModel = null;
            if (modelName != null) {
              //  sysModel = sysModelService.selectModelByName(modelName);
            }

            // 根据模型配置获取客户端
            OpenAiStreamClient openAiStreamClient;
            if (sysModel != null && StringUtils.isNotEmpty(sysModel.getApiHost()) && StringUtils.isNotEmpty(sysModel.getApiKey())) {
                openAiStreamClient = chatConfig.createOpenAiStreamClient(sysModel.getApiHost(), sysModel.getApiKey());
            } else {
                openAiStreamClient = chatConfig.getOpenAiStreamClient();
            }

            // 构建消息
            List<Message> messages = new ArrayList<>();

            // 添加模型特定系统提示词
            if (sysModel != null && StringUtils.isNotEmpty(sysModel.getSystemPrompt())) {
                messages.add(Message.builder()
                        .role(Message.Role.SYSTEM)
                        .content(sysModel.getSystemPrompt())
                        .build());
            }

            // 添加传入的系统提示词（如果未被模型特定提示词覆盖）
            if (StringUtils.isNotEmpty(systemPrompt) &&
                    (sysModel == null || StringUtils.isEmpty(sysModel.getSystemPrompt()))) {
                messages.add(Message.builder()
                        .role(Message.Role.SYSTEM)
                        .content(systemPrompt)
                        .build());
            }

            // 添加用户提示词
            messages.add(Message.builder()
                    .role(Message.Role.USER)
                    .content(userPrompt)
                    .build());

            // 获取请求参数
            double temperature = params.containsKey("temperature") ? ((Number) params.get("temperature")).doubleValue() : 0.7;
            double topP = params.containsKey("topP") ? ((Number) params.get("topP")).doubleValue() : 1.0;
            boolean stream = params.containsKey("stream") ? (boolean) params.get("stream") : false;

            // 根据流式与非流式选择不同的处理逻辑
            if (!stream) {
                // 非流式请求
                ChatCompletion completion = ChatCompletion.builder()
                        .messages(messages)
                        .model(modelName)
                        .temperature(temperature)
                        .topP(topP)
                        .build();

                ChatCompletionResponse response = openAiStreamClient.chatCompletion(completion);
                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                    return (String) response.getChoices().get(0).getMessage().getContent();
                }
                return "";
            } else {
                // 流式请求，使用CountDownLatch等待完成
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<StringBuilder> contentBuilder = new AtomicReference<>(new StringBuilder());

                ChatCompletion completion = ChatCompletion.builder()
                        .messages(messages)
                        .model(modelName)
                        .temperature(temperature)
                        .topP(topP)
                        .stream(true)
                        .build();

                openAiStreamClient.streamChatCompletion(completion, new EventSourceListener() {
                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        if (!"[DONE]".equals(data)) {
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                if (node.has("choices") &&
                                        node.get("choices").size() > 0 &&
                                        node.get("choices").get(0).has("delta") &&
                                        node.get("choices").get(0).get("delta").has("content")) {

                                    String content = node.get("choices").get(0).get("delta").get("content").asText();
                                    contentBuilder.get().append(content);
                                }
                            } catch (Exception e) {
                                log.error("解析模型响应失败", e);
                            }
                        }
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        log.error("流式请求失败", t);
                        if (response != null) {
                            log.error("响应状态码: {}", response.code());
                        }
                        latch.countDown();
                    }
                });

                // 等待响应完成，最多30秒
                try {
                    latch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待响应被中断", e);
                }

                return contentBuilder.get().toString();
            }
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            throw new RuntimeException("调用大语言模型失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String askLLM(String systemPrompt, String userPrompt, Map<String, Object> params) {
        try {
            // 获取客户端
            openAiStreamClient = chatConfig.getOpenAiStreamClient();

            // 构建消息
            List<Message> messages = new ArrayList<>();
            if (StringUtils.isNotEmpty(systemPrompt)) {
                messages.add(Message.builder().role(Message.Role.SYSTEM).content(systemPrompt).build());
            }
            messages.add(Message.builder().role(Message.Role.USER).content(userPrompt).build());

            // 获取请求参数
            String model = (String) params.getOrDefault("model", "gpt-3.5-turbo");
            double temperature = (double) params.getOrDefault("temperature", 0.7);
            double topP = (double) params.getOrDefault("topP", 1.0);
            boolean stream = (boolean) params.getOrDefault("stream", false);

            if (!stream) {
                // 非流式请求
                ChatCompletion completion = ChatCompletion.builder()
                        .messages(messages)
                        .model(model)
                        .temperature(temperature)
                        .topP(topP)
                        .build();

                ChatCompletionResponse response = openAiStreamClient.chatCompletion(completion);
                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                    return (String) response.getChoices().get(0).getMessage().getContent();
                }
                return "";
            } else {
                // 流式请求，使用CountDownLatch等待完成
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<StringBuilder> contentBuilder = new AtomicReference<>(new StringBuilder());

                ChatCompletion completion = ChatCompletion.builder()
                        .messages(messages)
                        .model(model)
                        .temperature(temperature)
                        .topP(topP)
                        .stream(true)
                        .build();

                openAiStreamClient.streamChatCompletion(completion, new EventSourceListener() {
                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        if (!"[DONE]".equals(data)) {
                            try {
                                // 假设有一个工具方法从data中提取content
                                String content = extractContentFromData(data);
                                contentBuilder.get().append(content);
                            } catch (Exception e) {
                                log.error("解析响应失败", e);
                            }
                        }
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        log.error("流式请求失败", t);
                        latch.countDown();
                    }
                });

                // 等待响应完成，最多30秒
                try {
                    latch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待响应被中断", e);
                }

                return contentBuilder.get().toString();
            }
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            throw new RuntimeException("调用大语言模型失败: " + e.getMessage(), e);
        }
    }

    // 工具方法：从响应数据中提取content
    private String extractContentFromData(String data) {
        try {
            // 这里需要根据实际的响应格式进行解析
            // 假设使用JSON格式
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            if (node.has("choices") && node.get("choices").size() > 0 &&
                    node.get("choices").get(0).has("delta") &&
                    node.get("choices").get(0).get("delta").has("content")) {
                return node.get("choices").get(0).get("delta").get("content").asText();
            }
        } catch (Exception e) {
            log.error("解析内容失败", e);
        }
        return "";
    }
}