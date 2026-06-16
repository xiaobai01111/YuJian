package com.campus.wall.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * JVM 级单例容器，避免每个测试类重建端口。
 */
public final class SingletonPostgresContainer extends PostgreSQLContainer<SingletonPostgresContainer> {

    private static final String IMAGE = "postgres:16-alpine";
    private static final SingletonPostgresContainer INSTANCE = new SingletonPostgresContainer();

    private SingletonPostgresContainer() {
        super(IMAGE);
        withDatabaseName("campus_test");
        withUsername("campus");
        withPassword("campus");
    }

    public static SingletonPostgresContainer getInstance() {
        return INSTANCE;
    }

    @Override
    public void stop() {
        // Keep container running for the JVM lifetime.
    }
}
