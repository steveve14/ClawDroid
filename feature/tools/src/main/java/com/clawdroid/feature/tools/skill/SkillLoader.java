package com.clawdroid.feature.tools.skill;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SkillLoader {

    private static final String SKILLS_DIR_NAME = "skills";
    private final Context context;

    @Inject
    public SkillLoader(@ApplicationContext Context context) {
        this.context = context;
    }

    public List<Skill> loadSkills() {
        // Use app-internal storage to prevent other apps from injecting malicious skills
        File skillsDir = new File(context.getFilesDir(), SKILLS_DIR_NAME);
        if (!skillsDir.exists() || !skillsDir.isDirectory()) {
            return Collections.emptyList();
        }

        List<Skill> skills = new ArrayList<>();
        File[] dirs = skillsDir.listFiles(File::isDirectory);
        if (dirs == null) return skills;

        for (File dir : dirs) {
            File skillFile = new File(dir, "SKILL.md");
            if (skillFile.exists()) {
                Skill skill = parseSkillFile(skillFile, dir.getAbsolutePath());
                if (skill != null) skills.add(skill);
            }
        }
        return skills;
    }

    private Skill parseSkillFile(File file, String directory) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            String text = content.toString();
            String frontMatter = extractFrontMatter(text);
            if (frontMatter == null) return null;

            String name = extractField(frontMatter, "name");
            String description = extractField(frontMatter, "description");
            String version = extractField(frontMatter, "version");
            String author = extractField(frontMatter, "author");
            List<String> tools = extractListField(frontMatter, "tools");

            // Extract system prompt extension (after front matter)
            String body = text.substring(text.indexOf("---", 3) + 3).trim();
            String systemPrompt = extractSection(body, "시스템 프롬프트 확장");

            if (name == null || name.isEmpty()) {
                name = new File(directory).getName();
            }

            return new Skill(name, description, version, author, tools,
                    systemPrompt, directory);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFrontMatter(String text) {
        if (!text.startsWith("---")) return null;
        int end = text.indexOf("---", 3);
        if (end < 0) return null;
        return text.substring(3, end).trim();
    }

    private String extractField(String frontMatter, String field) {
        Pattern pattern = Pattern.compile(field + ":\\s*(.+)");
        Matcher matcher = pattern.matcher(frontMatter);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private List<String> extractListField(String frontMatter, String field) {
        List<String> items = new ArrayList<>();
        Pattern pattern = Pattern.compile(field + ":\\s*\\n((?:\\s+-\\s+.+\\n?)+)");
        Matcher matcher = pattern.matcher(frontMatter);
        if (matcher.find()) {
            String listStr = matcher.group(1);
            Pattern itemPattern = Pattern.compile("-\\s+(.+)");
            Matcher itemMatcher = itemPattern.matcher(listStr);
            while (itemMatcher.find()) {
                items.add(itemMatcher.group(1).trim());
            }
        }
        return items;
    }

    private String extractSection(String body, String sectionHeader) {
        int idx = body.indexOf("## " + sectionHeader);
        if (idx < 0) return null;
        int start = body.indexOf("\n", idx);
        if (start < 0) return null;
        int end = body.indexOf("\n## ", start + 1);
        if (end < 0) end = body.length();
        return body.substring(start, end).trim();
    }
}
