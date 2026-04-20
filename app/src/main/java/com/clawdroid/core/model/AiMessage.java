package com.clawdroid.core.model;

import java.util.List;

public class AiMessage {
    private final String role;
    private final String content;
    private final List<byte[]> images;

    public AiMessage(String role, String content) {
        this(role, content, null);
    }

    public AiMessage(String role, String content, List<byte[]> images) {
        this.role = role;
        this.content = content;
        this.images = images;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
    public List<byte[]> getImages() { return images; }
}
