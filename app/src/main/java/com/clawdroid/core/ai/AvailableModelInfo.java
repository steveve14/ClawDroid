package com.clawdroid.core.ai;

public class AvailableModelInfo {
    public final String providerId;
    public final String providerName;
    public final String modelId;
    public final String modelName;

    public AvailableModelInfo(String providerId, String providerName,
                              String modelId, String modelName) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.modelId = modelId;
        this.modelName = modelName;
    }
}
