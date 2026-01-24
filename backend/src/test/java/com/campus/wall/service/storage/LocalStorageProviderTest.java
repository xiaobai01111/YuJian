package com.campus.wall.service.storage;

import com.campus.wall.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalStorageProviderTest {

    @Test
    void store_rejectsPathTraversal() throws IOException {
        StorageProperties props = new StorageProperties();
        Path tempDir = Files.createTempDirectory("local-store-test");
        props.setLocalPath(tempDir.toString());

        LocalStorageProvider provider = new LocalStorageProvider(props);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );

        assertThrows(IOException.class, () -> provider.store(file, "../evil.txt"));
    }
}
