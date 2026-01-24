package com.campus.wall.service.file;

import com.campus.wall.common.BusinessException;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.service.storage.StorageProviderRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileAccessServiceTest {

    @Test
    void verifySignature_acceptsValidSig() {
        StorageProperties props = new StorageProperties();
        props.setSigningSecret("test-secret");
        props.setPrivateUrlTtlSeconds(600);
        FileAccessService service = new FileAccessService(new StorageProviderRegistry(List.of()), props);

        String url = service.buildSignedPreviewUrl(100L);
        Map<String, String> query = parseQuery(url);
        long expires = Long.parseLong(query.get("expires"));
        String sig = query.get("sig");

        assertDoesNotThrow(() -> service.verifySignature(100L, expires, sig));
    }

    @Test
    void verifySignature_rejectsInvalidSig() {
        StorageProperties props = new StorageProperties();
        props.setSigningSecret("test-secret");
        props.setPrivateUrlTtlSeconds(600);
        FileAccessService service = new FileAccessService(new StorageProviderRegistry(List.of()), props);

        String url = service.buildSignedPreviewUrl(200L);
        Map<String, String> query = parseQuery(url);
        long expires = Long.parseLong(query.get("expires"));

        assertThrows(BusinessException.class, () -> service.verifySignature(200L, expires, "bad"));
    }

    @Test
    void verifySignature_rejectsExpiredSig() {
        StorageProperties props = new StorageProperties();
        props.setSigningSecret("test-secret");
        props.setPrivateUrlTtlSeconds(600);
        FileAccessService service = new FileAccessService(new StorageProviderRegistry(List.of()), props);

        long expires = System.currentTimeMillis() / 1000 - 1;
        assertThrows(BusinessException.class, () -> service.verifySignature(300L, expires, "sig"));
    }

    private Map<String, String> parseQuery(String url) {
        Map<String, String> result = new HashMap<>();
        int idx = url.indexOf('?');
        assertThat(idx).isGreaterThan(0);
        String query = url.substring(idx + 1);
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            result.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return result;
    }
}
