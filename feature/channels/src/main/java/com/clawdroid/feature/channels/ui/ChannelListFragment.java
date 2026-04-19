package com.clawdroid.feature.channels.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clawdroid.feature.channels.databinding.FragmentChannelListBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChannelListFragment extends Fragment {

    private FragmentChannelListBinding binding;
    private ChannelListViewModel viewModel;
    private ChannelAdapter adapter;

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
        viewModel = new ViewModelProvider(this).get(ChannelListViewModel.class);

        adapter = new ChannelAdapter(channel -> {
            Bundle args = new Bundle();
            args.putString("channelId", channel.getId());
            Navigation.findNavController(view)
                    .navigate(com.clawdroid.core.ui.R.id.action_channelList_to_channelDetail, args);
        });
        binding.recyclerChannels.setAdapter(adapter);

        viewModel.getChannels().observe(getViewLifecycleOwner(), channels -> {
            if (channels == null || channels.isEmpty()) {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.recyclerChannels.setVisibility(View.GONE);
            } else {
                binding.emptyState.setVisibility(View.GONE);
                binding.recyclerChannels.setVisibility(View.VISIBLE);
                adapter.submitList(channels);
            }
        });

        binding.fabAddChannel.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(com.clawdroid.core.ui.R.id.action_channelList_to_channelAdd)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
