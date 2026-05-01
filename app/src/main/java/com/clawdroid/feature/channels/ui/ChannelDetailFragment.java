package com.clawdroid.feature.channels.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.app.R;
import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.entity.ChannelEntity;
import com.clawdroid.feature.channels.channel.ChannelManager;
import com.clawdroid.feature.channels.channel.MessageRouter;
import com.clawdroid.app.databinding.FragmentChannelDetailBinding;

import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class ChannelDetailFragment extends Fragment {

    private static final int MAX_CHANNEL_SYSTEM_PROMPT_LENGTH = 2000;

    private FragmentChannelDetailBinding binding;
    @Inject ChannelDao channelDao;
    @Inject ChannelManager channelManager;
    @Inject MessageRouter messageRouter;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private ChannelEntity currentChannel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChannelDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        String channelId = getArguments() != null
                ? getArguments().getString("channelId") : null;
        if (channelId == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        loadChannel(channelId, view);
    }

    private void loadChannel(String channelId, View view) {
        disposables.add(
                channelDao.getById(channelId)
                        .subscribeOn(Schedulers.io())
                        .subscribe(channel -> {
                            currentChannel = channel;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (isViewActive()) {
                                        bindChannel(channel, view);
                                    }
                                });
                            }
                        }, e -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (isViewActive()) {
                                        Navigation.findNavController(view).popBackStack();
                                    }
                                });
                            }
                        })
        );
    }

    private void bindChannel(ChannelEntity channel, View view) {
        if (!isViewActive()) return;
        binding.tvChannelName.setText(channel.getName());

        String typeLabel;
        switch (channel.getType()) {
            case "telegram": typeLabel = "✈️ Telegram"; break;
            case "discord": typeLabel = "🎮 Discord"; break;
            case "slack": typeLabel = "💼 Slack"; break;
            case "gateway": typeLabel = "🌐 Gateway"; break;
            default: typeLabel = "📡 " + channel.getType(); break;
        }
        binding.tvChannelType.setText(typeLabel);

        updateStatus(channel.getStatus());

        binding.etChannelSystemPrompt.setText(
            channel.getSystemPrompt() != null ? channel.getSystemPrompt() : "");
        binding.btnSavePersonaPrompt.setOnClickListener(v -> saveChannelPersonaPrompt(view));

        boolean isConnected = "connected".equals(channel.getStatus());
    binding.btnToggleConnection.setText(isConnected
        ? getString(R.string.channel_disconnect)
        : getString(R.string.channel_connect));

        binding.btnToggleConnection.setOnClickListener(v -> {
            if ("connected".equals(currentChannel.getStatus())) {
                disposables.add(
                        channelManager.disconnectChannel(channel.getId())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> reloadChannel(channel.getId(), view),
                                        e -> showError(e.getMessage())
                                )
                );
            } else {
                disposables.add(
                        channelManager.connectChannel(channel)
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> reloadChannel(channel.getId(), view),
                                        e -> showError(e.getMessage())
                                )
                );
            }
        });

        // Pairing
        boolean showPairing = "pairing".equals(channel.getDmPolicy());
        binding.cardPairing.setVisibility(showPairing ? View.VISIBLE : View.GONE);
        binding.btnGenerateCode.setOnClickListener(v -> {
            String code = messageRouter.generatePairingCode(channel.getId());
            binding.tvPairingCode.setText(code);
        });

        binding.btnDelete.setOnClickListener(v -> {
            disposables.add(
                    channelManager.disconnectChannel(channel.getId())
                            .andThen(channelDao.delete(channel))
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    () -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                if (isViewActive()) {
                                                    Navigation.findNavController(view).popBackStack();
                                                }
                                            });
                                        }
                                    },
                                    e -> showError(e.getMessage())
                            )
            );
        });
    }

    private void updateStatus(String status) {
        if (!isViewActive()) return;
        String statusText;
        int statusColor;
        switch (status) {
            case "connected":
                statusText = getString(R.string.channel_status_connected);
                statusColor = 0xFF4CAF50;
                break;
            case "error":
                statusText = getString(R.string.channel_status_error);
                statusColor = 0xFFF44336;
                break;
            default:
                statusText = getString(R.string.channel_status_disconnected);
                statusColor = 0xFF9E9E9E;
                break;
        }
        binding.tvStatus.setText(statusText);
        binding.statusDot.getBackground().setTint(statusColor);
    }

    private void reloadChannel(String channelId, View view) {
        disposables.add(
                channelDao.getById(channelId)
                        .subscribeOn(Schedulers.io())
                        .subscribe(channel -> {
                            currentChannel = channel;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (isViewActive()) {
                                        bindChannel(channel, view);
                                    }
                                });
                            }
                        }, e -> showError(e.getMessage()))
        );
    }

    private void saveChannelPersonaPrompt(View view) {
        if (currentChannel == null || binding == null) return;
        String prompt = binding.etChannelSystemPrompt.getText() != null
                ? binding.etChannelSystemPrompt.getText().toString().trim()
                : "";
        if (prompt.length() > MAX_CHANNEL_SYSTEM_PROMPT_LENGTH) {
            showError(getString(R.string.channel_persona_too_long));
            return;
        }
        String channelId = currentChannel.getId();
        currentChannel.setSystemPrompt(prompt.isEmpty() ? null : prompt);
        currentChannel.setUpdatedAt(Instant.now().toString());

        disposables.add(
                channelDao.update(currentChannel)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                () -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            if (isAdded()) {
                                                Toast.makeText(requireContext(), R.string.channel_persona_saved, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                    reloadChannel(channelId, view);
                                },
                                e -> showError(e.getMessage())
                        )
        );
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.common_error_prefix, message),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isViewActive() {
        return binding != null && isAdded();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}
