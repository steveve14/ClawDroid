package com.clawdroid.feature.tools.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.feature.tools.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ToolSettingsFragment extends Fragment {

    private ToolSettingsViewModel viewModel;
    private ToolSettingsAdapter adapter;
    private SkillSettingsAdapter skillAdapter;
    private RecyclerView recycler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tool_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ToolSettingsViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recycler = view.findViewById(R.id.recyclerTools);
        adapter = new ToolSettingsAdapter();
        adapter.setOnToggleListener((name, enabled) -> viewModel.toggleTool(name, enabled));

        skillAdapter = new SkillSettingsAdapter();
        skillAdapter.setOnToggleListener((name, enabled) -> viewModel.toggleSkill(name, enabled));

        recycler.setAdapter(adapter);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showTools();
                } else {
                    showSkills();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewModel.tools.observe(getViewLifecycleOwner(), tools -> {
            viewModel.enabledTools.observe(getViewLifecycleOwner(), enabled -> {
                adapter.setTools(tools, enabled);
            });
        });

        viewModel.skills.observe(getViewLifecycleOwner(), skills -> {
            if (skills != null) {
                java.util.Set<String> enabled = new java.util.HashSet<>();
                for (com.clawdroid.feature.tools.skill.Skill s : skills) {
                    if (viewModel.isSkillEnabled(s.getName())) {
                        enabled.add(s.getName());
                    }
                }
                skillAdapter.setSkills(skills, enabled);
            }
        });
    }

    private void showTools() {
        recycler.setAdapter(adapter);
    }

    private void showSkills() {
        recycler.setAdapter(skillAdapter);
    }
}
