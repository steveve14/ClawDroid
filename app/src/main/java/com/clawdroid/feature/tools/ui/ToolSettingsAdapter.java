package com.clawdroid.feature.tools.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.app.R;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ToolSettingsAdapter extends RecyclerView.Adapter<ToolSettingsAdapter.ViewHolder> {

    public interface OnToggleListener {
        void onToggle(String toolName, boolean enabled);
    }

    private final List<Tool> tools = new ArrayList<>();
    private final Set<String> enabledTools = new HashSet<>();
    private OnToggleListener listener;

    public void setTools(List<Tool> tools, Set<String> enabled) {
        this.tools.clear();
        this.tools.addAll(tools);
        this.enabledTools.clear();
        this.enabledTools.addAll(enabled);
        notifyDataSetChanged();
    }

    public void setOnToggleListener(OnToggleListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tool_setting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tool tool = tools.get(position);
        holder.tvName.setText(tool.getName());
        holder.tvDescription.setText(tool.getDescription());
        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(enabledTools.contains(tool.getName()));
        holder.switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) enabledTools.add(tool.getName());
            else enabledTools.remove(tool.getName());
            if (listener != null) listener.onToggle(tool.getName(), checked);
        });
    }

    @Override
    public int getItemCount() { return tools.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDescription;
        final MaterialSwitch switchEnabled;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvToolName);
            tvDescription = view.findViewById(R.id.tvToolDescription);
            switchEnabled = view.findViewById(R.id.switchEnabled);
        }
    }
}
