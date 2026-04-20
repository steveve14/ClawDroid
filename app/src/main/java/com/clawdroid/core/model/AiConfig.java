package com.clawdroid.core.model;

public class AiConfig {
    private float temperature = 0.7f;
    private float topP = 0.95f;
    private int topK = 40;
    private int maxOutputTokens = 4096;

    public AiConfig() {}

    public AiConfig(float temperature, float topP, int topK, int maxOutputTokens) {
        this.temperature = temperature;
        this.topP = topP;
        this.topK = topK;
        this.maxOutputTokens = maxOutputTokens;
    }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getTopP() { return topP; }
    public void setTopP(float topP) { this.topP = topP; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
}
