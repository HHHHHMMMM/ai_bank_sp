package org.ruoyi.knowledgegraph.service.impl;

import java.util.Map;

/**
 * 大语言模型服务接口
 * 提供与LLM交互的抽象层
 */
public interface LLMService {

    /**
     * 向LLM提问并获取回答
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param modelName 模型名称
     * @param params 其他请求参数，如温度等
     * @return LLM的响应文本
     */
    String askLLM(String systemPrompt, String userPrompt, String modelName, Map<String, Object> params);

    /**
     * 使用默认模型向LLM提问并获取回答
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param params 其他请求参数，如温度等
     * @return LLM的响应文本
     */
    String askLLM(String systemPrompt, String userPrompt, Map<String, Object> params);
}