package com.clawdroid.feature.tools.skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SkillManager {

    private final SkillLoader skillLoader;
    private final BuiltinSkillInstaller builtinInstaller;
    private final Map<String, Skill> skills = new LinkedHashMap<>();
    private final Map<String, Boolean> enabledState = new LinkedHashMap<>();

    @Inject
    public SkillManager(SkillLoader skillLoader, BuiltinSkillInstaller builtinInstaller) {
        this.skillLoader = skillLoader;
        this.builtinInstaller = builtinInstaller;
    }

    public void loadSkills() {
        builtinInstaller.installIfNeeded();
        skills.clear();
        List<Skill> loaded = skillLoader.loadSkills();
        for (Skill skill : loaded) {
            skills.put(skill.getName(), skill);
            enabledState.putIfAbsent(skill.getName(), true);
        }
    }

    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    public List<Skill> getEnabledSkills() {
        List<Skill> enabled = new ArrayList<>();
        for (Map.Entry<String, Skill> entry : skills.entrySet()) {
            if (Boolean.TRUE.equals(enabledState.get(entry.getKey()))) {
                enabled.add(entry.getValue());
            }
        }
        return enabled;
    }

    public void setEnabled(String name, boolean enabled) {
        enabledState.put(name, enabled);
    }

    public boolean isEnabled(String name) {
        return Boolean.TRUE.equals(enabledState.get(name));
    }

    public String buildSystemPromptExtensions() {
        StringBuilder sb = new StringBuilder();
        for (Skill skill : getEnabledSkills()) {
            if (skill.getSystemPromptExtension() != null) {
                sb.append("\n\n### Skill: ").append(skill.getName()).append("\n");
                sb.append(skill.getSystemPromptExtension());
            }
        }
        return sb.toString();
    }
}
