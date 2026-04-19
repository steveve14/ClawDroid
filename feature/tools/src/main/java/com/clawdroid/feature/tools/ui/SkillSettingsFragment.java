package com.clawdroid.feature.tools.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.feature.tools.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashSet;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SkillSettingsFragment extends Fragment {

    private ToolSettingsViewModel viewModel;
    private SkillSettingsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_skill_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ToolSettingsViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        RecyclerView recycler = view.findViewById(R.id.recyclerSkills);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyState);

        adapter = new SkillSettingsAdapter();
        adapter.setOnToggleListener((name, enabled) -> viewModel.toggleSkill(name, enabled));
        recycler.setAdapter(adapter);

        viewModel.skills.observe(getViewLifecycleOwner(), skills -> {
            if (skills == null || skills.isEmpty()) {
                recycler.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                recycler.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);

                Set<String> enabled = new HashSet<>();
                for (var skill : skills) {
                    if (viewModel.isSkillEnabled(skill.getName())) {
                        enabled.add(skill.getName());
                    }
                }
                adapter.setSkills(skills, enabled);
            }
        });
    }
}
