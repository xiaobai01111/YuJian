package com.campus.wall.service.file;

import com.campus.wall.common.BusinessException;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.mapper.file.FileRecordMapper;
import com.campus.wall.service.storage.StorageProviderRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        FileRecordMapper fileRecordMapper = Mockito.mock(FileRecordMapper.class);
        FileAccessService service = new FileAccessService(new StorageProviderRegistry(List.of()), props, fileRecordMapper);

        String publicKey = "pk100";
        String url = service.buildSignedPreviewUrl(publicKey);
        Map<String, String> query = parseQuery(url);
        long expires = Long.parseLong(query.get("expires"));
        String sig = query.get("sig");

        assertDoesNotThrow(() -> service.verifySignature(publicKey, expires, sig));
    }

    @Test
    void verifySignature_withBindUserId_acceptsMatchingUidAndRejectsMismatchedUid() {
        StorageProperties props = new StorageProperties();
        props.setSigningSecret("test-secret");
        props.setPrivateUrlTtlSeconds(600);
        FileRecordMapper fileRecordMapper = Mockito.mock(FileRecordMapper.class);
        FileAccessService service = new FileAccessService(new StorageProviderRegistry(List.of()), props, fileRecordMapper);

        String publicKey = "pk-bind";
        String url = service.buildSignedPreviewUrl(publicKey, 99L);
        Map<String, String> query = parseQuery(url);
        long expires = Long.parseLong(query.get("expires"));
        String sig = query.get("sig");

        assertThat(query.get("uid")).isEqualTo("99");
        assertDoesNotThrow(() -> service.verifySignature(publicKey, expires, sig, 99L));
        assertThrows(BusinessException.class, () -> service.verifySignature(publicKey, expires, sig, 98L));
        assertThrows(BusinessException.class, () -> service.verifySignature(publicKey, expires, sig));
    }

    @Test
    void verifySignature_rejectsInvalidSig() {
        StorageProperties props = new StorageProperties();
        props.setSigningSecret("test-secret");
        props.setPrivateUrlTtlSeconds(600);
        FileRecordMapper fileRecordMapper = Mockito.mock(FileRecordMapper.class);
        FileAccessService service = new FileAccessService(new StorageProviderRegistry(List.of()), props, fileRecordMapper);

        String publicKey = "pk200";
        String url = service.buildSignedPreviewUrl(publicKey);
        Map<String, String> query = parseQuery(url);
        long expires = Long.parseLong(query.get("expires"));

        assertThrows(BusinessException.class, () -> service.verifySignature(publicKey, expires, "bad"));
    }

    @Test
    void verifySignature_rejectsExpiredSig() {
        StorageProperties props = new StorageProperties();
        props.setSigningSecret("test-secret");
        props.setPrivateUrlTtlSeconds(600);
        FileRecordMapper fileRecordMapper = Mockito.mock(FileRecordMapper.class);
        FileAccessService service = new FileAccessService(new StorageProviderRegistry(List.of()), props, fileRecordMapper);

        long expires = System.currentTimeMillis() / 1000 - 1;
        assertThrows(BusinessException.class, () -> service.verifySignature("pk300", expires, "sig"));
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
