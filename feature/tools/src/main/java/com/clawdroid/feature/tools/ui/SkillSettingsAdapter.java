package com.clawdroid.feature.tools.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.feature.tools.R;
import com.clawdroid.feature.tools.skill.Skill;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillSettingsAdapter extends RecyclerView.Adapter<SkillSettingsAdapter.ViewHolder> {

    public interface OnToggleListener {
        void onToggle(String skillName, boolean enabled);
    }

    private final List<Skill> skills = new ArrayList<>();
    private final Set<String> enabledSkills = new HashSet<>();
    private OnToggleListener listener;

    public void setSkills(List<Skill> skills, Set<String> enabled) {
        this.skills.clear();
        this.skills.addAll(skills);
        this.enabledSkills.clear();
        this.enabledSkills.addAll(enabled);
        notifyDataSetChanged();
    }

    public void setOnToggleListener(OnToggleListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_skill_setting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Skill skill = skills.get(position);
        holder.tvName.setText(skill.getName());
        holder.tvDescription.setText(skill.getDescription() != null
                ? skill.getDescription() : "설명 없음");

        String meta = buildMeta(skill);
        holder.tvMeta.setText(meta);
        holder.tvMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(enabledSkills.contains(skill.getName()));
        holder.switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) enabledSkills.add(skill.getName());
            else enabledSkills.remove(skill.getName());
            if (listener != null) listener.onToggle(skill.getName(), checked);
        });
    }

    private String buildMeta(Skill skill) {
        StringBuilder sb = new StringBuilder();
        if (skill.getVersion() != null) sb.append("v").append(skill.getVersion());
        if (skill.getAuthor() != null) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(skill.getAuthor());
        }
        if (skill.getTools() != null && !skill.getTools().isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("도구: ").append(String.join(", ", skill.getTools()));
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return skills.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDescription;
        final TextView tvMeta;
        final MaterialSwitch switchEnabled;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvSkillName);
            tvDescription = view.findViewById(R.id.tvSkillDescription);
            tvMeta = view.findViewById(R.id.tvSkillMeta);
            switchEnabled = view.findViewById(R.id.switchSkillEnabled);
        }
    }
}
