package com.campus.wall.support;

import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SaTokenTestContext {
    private SaTokenTestContext() {
    }

    public static void bind() {
        SaTokenContextForThreadLocalStorage.setBox(
                new SimpleRequest(),
                new SimpleResponse(),
                new SimpleStorage()
        );
    }

    public static void clear() {
        SaTokenContextForThreadLocalStorage.clearBox();
    }

    private static final class SimpleRequest implements SaRequest {
        @Override
        public Object getSource() {
            return null;
        }

        @Override
        public String getParam(String name) {
            return null;
        }

        @Override
        public List<String> getParamNames() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> getParamMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public String getRequestPath() {
            return "";
        }

        @Override
        public String getUrl() {
            return "";
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public Object forward(String path) {
            return null;
        }
    }

    private static final class SimpleResponse implements SaResponse {
        @Override
        public Object getSource() {
            return null;
        }

        @Override
        public SaResponse setStatus(int sc) {
            return this;
        }

        @Override
        public SaResponse setHeader(String name, String value) {
            return this;
        }

        @Override
        public SaResponse addHeader(String name, String value) {
            return this;
        }

        @Override
        public Object redirect(String url) {
            return null;
        }
    }

    private static final class SimpleStorage implements SaStorage {
        private final Map<String, Object> data = new HashMap<>();

        @Override
        public Object getSource() {
            return null;
        }

        @Override
        public Object get(String key) {
            return data.get(key);
        }

        @Override
        public SaStorage set(String key, Object value) {
            data.put(key, value);
            return this;
        }

        @Override
        public SaStorage delete(String key) {
            data.remove(key);
            return this;
        }
    }
}
