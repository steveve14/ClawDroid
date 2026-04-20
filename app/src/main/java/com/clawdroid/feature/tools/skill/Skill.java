package com.clawdroid.feature.tools.skill;

import java.util.List;

public class Skill {
    private final String name;
    private final String description;
    private final String version;
    private final String author;
    private final List<String> tools;
    private final String systemPromptExtension;
    private final String directory;

    public Skill(String name, String description, String version, String author,
                 List<String> tools, String systemPromptExtension, String directory) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
        this.tools = tools;
        this.systemPromptExtension = systemPromptExtension;
        this.directory = directory;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public List<String> getTools() { return tools; }
    public String getSystemPromptExtension() { return systemPromptExtension; }
    public String getDirectory() { return directory; }
}
