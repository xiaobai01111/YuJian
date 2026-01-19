package com.campus.wall.service.storage;

import com.campus.wall.enums.file.StorageProviderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class StorageProviderRegistry {

    private final Map<StorageProviderType, StorageProvider> providers = new EnumMap<>(StorageProviderType.class);

    public StorageProviderRegistry(List<StorageProvider> providerList) {
        for (StorageProvider provider : providerList) {
            providers.put(provider.getType(), provider);
        }
    }

    public StorageProvider getProvider(StorageProviderType type) {
        return providers.get(type);
    }
}
