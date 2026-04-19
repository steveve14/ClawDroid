package com.clawdroid.feature.channels.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clawdroid.feature.channels.databinding.FragmentChannelListBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChannelListFragment extends Fragment {

    private FragmentChannelListBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChannelListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show empty state for now
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerChannels.setVisibility(View.GONE);

        binding.fabAddChannel.setOnClickListener(v -> {
            // TODO: Navigate to channel add screen
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
