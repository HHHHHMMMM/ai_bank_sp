package org.ruoyi.knowledgegraph.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.ruoyi.system.service.impl"})
public class ServiceConfig {
    // 这个配置确保能找到module-a中的服务实现
}