package com.campus.wall.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MinioEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String primary = env.getProperty("app.storage.primary-provider", "LOCAL");
        String fallback = env.getProperty("app.storage.fallback-provider", "LOCAL");
        return "MINIO".equalsIgnoreCase(primary) || "MINIO".equalsIgnoreCase(fallback);
    }
}
