package com.clawdroid.feature.tools.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clawdroid.feature.tools.skill.Skill;
import com.clawdroid.feature.tools.skill.SkillManager;
import com.clawdroid.feature.tools.tool.Tool;
import com.clawdroid.feature.tools.tool.ToolRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ToolSettingsViewModel extends ViewModel {

    private final ToolRegistry toolRegistry;
    private final SkillManager skillManager;

    private final MutableLiveData<List<Tool>> _tools = new MutableLiveData<>();
    public final LiveData<List<Tool>> tools = _tools;

    private final MutableLiveData<Set<String>> _enabledTools = new MutableLiveData<>();
    public final LiveData<Set<String>> enabledTools = _enabledTools;

    private final MutableLiveData<List<Skill>> _skills = new MutableLiveData<>();
    public final LiveData<List<Skill>> skills = _skills;

    @Inject
    public ToolSettingsViewModel(ToolRegistry toolRegistry, SkillManager skillManager) {
        this.toolRegistry = toolRegistry;
        this.skillManager = skillManager;
        load();
    }

    private void load() {
        _tools.setValue(toolRegistry.getAllTools());
        Set<String> enabled = new HashSet<>();
        for (Tool t : toolRegistry.getAllTools()) {
            if (toolRegistry.isEnabled(t.getName())) enabled.add(t.getName());
        }
        _enabledTools.setValue(enabled);

        skillManager.loadSkills();
        _skills.setValue(skillManager.getAllSkills());
    }

    public void toggleTool(String name, boolean enabled) {
        toolRegistry.setEnabled(name, enabled);
    }

    public void toggleSkill(String name, boolean enabled) {
        skillManager.setEnabled(name, enabled);
    }

    public boolean isSkillEnabled(String name) {
        return skillManager.isEnabled(name);
    }
}
