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

import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.entity.ChannelEntity;
import com.clawdroid.app.databinding.FragmentChannelAddBinding;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class ChannelAddFragment extends Fragment {

    private FragmentChannelAddBinding binding;
    @Inject ChannelDao channelDao;
    @Inject Gson gson;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChannelAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        binding.rgChannelType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isTelegramOrDiscord = checkedId == binding.rbTelegram.getId()
                    || checkedId == binding.rbDiscord.getId();
            boolean isSlack = checkedId == binding.rbSlack.getId();
            boolean isGateway = checkedId == binding.rbGateway.getId();

            binding.tilBotToken.setVisibility(
                    (isTelegramOrDiscord || isSlack) ? View.VISIBLE : View.GONE);
            binding.tilAppToken.setVisibility(isSlack ? View.VISIBLE : View.GONE);
            binding.tilServerUrl.setVisibility(isGateway ? View.VISIBLE : View.GONE);

            if (isGateway) {
                binding.tilBotToken.setHint("Auth Token");
            } else {
                binding.tilBotToken.setHint("Bot Token");
            }
        });

        binding.btnSave.setOnClickListener(v -> saveChannel(view));
    }

    private void saveChannel(View view) {
        String name = binding.etChannelName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.etChannelName.setError("이름을 입력하세요");
            return;
        }

        String type;
        int checkedId = binding.rgChannelType.getCheckedRadioButtonId();
        if (checkedId == binding.rbTelegram.getId()) type = "telegram";
        else if (checkedId == binding.rbDiscord.getId()) type = "discord";
        else if (checkedId == binding.rbSlack.getId()) type = "slack";
        else type = "gateway";

        JsonObject config = new JsonObject();
        String botToken = binding.etBotToken.getText().toString().trim();
        if (!botToken.isEmpty()) {
            config.addProperty("bot_token", botToken);
        }
        if ("slack".equals(type)) {
            String appToken = binding.etAppToken.getText().toString().trim();
            if (!appToken.isEmpty()) config.addProperty("app_token", appToken);
        }
        if ("gateway".equals(type)) {
            String serverUrl = binding.etServerUrl.getText().toString().trim();
            if (!serverUrl.isEmpty()) config.addProperty("server_url", serverUrl);
            if (!botToken.isEmpty()) config.addProperty("auth_token", botToken);
        }

        String dmPolicy;
        if (binding.chipOpen.isChecked()) dmPolicy = "open";
        else if (binding.chipClosed.isChecked()) dmPolicy = "closed";
        else dmPolicy = "pairing";

        ChannelEntity entity = new ChannelEntity();
        entity.setName(name);
        entity.setType(type);
        entity.setConfig(gson.toJson(config));
        entity.setDmPolicy(dmPolicy);
        entity.setStatus("disconnected");
        String now = Instant.now().toString();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        disposables.add(
                channelDao.insert(entity)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                () -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "채널이 추가되었습니다", Toast.LENGTH_SHORT).show();
                                            Navigation.findNavController(view).popBackStack();
                                        });
                                    }
                                },
                                e -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), "오류: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    }
                                }
                        )
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}
