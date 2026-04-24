package com.clawdroid.feature.channels.ui;

import android.graphics.Color;
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

import java.io.IOException;
import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@AndroidEntryPoint
public class ChannelAddFragment extends Fragment {

    private FragmentChannelAddBinding binding;
    @Inject ChannelDao channelDao;
    @Inject Gson gson;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final OkHttpClient http = new OkHttpClient();

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

            resetTestState();
        });

        // 입력 변경 시 테스트 상태 초기화
        android.text.TextWatcher invalidateOnChange = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { resetTestState(); }
        };
        binding.etBotToken.addTextChangedListener(invalidateOnChange);
        binding.etAppToken.addTextChangedListener(invalidateOnChange);
        binding.etServerUrl.addTextChangedListener(invalidateOnChange);

        binding.btnTestConnection.setOnClickListener(v -> testConnection());
        binding.btnSave.setOnClickListener(v -> saveChannel(view));
    }

    private void resetTestState() {
        if (binding == null) return;
        binding.btnSave.setEnabled(false);
        binding.tvTestResult.setVisibility(View.GONE);
    }

    private String currentType() {
        int checkedId = binding.rgChannelType.getCheckedRadioButtonId();
        if (checkedId == binding.rbTelegram.getId()) return "telegram";
        if (checkedId == binding.rbDiscord.getId()) return "discord";
        if (checkedId == binding.rbSlack.getId()) return "slack";
        return "gateway";
    }

    private void testConnection() {
        String type = currentType();
        String botToken = binding.etBotToken.getText().toString().trim();
        String appToken = binding.etAppToken.getText().toString().trim();
        String serverUrl = binding.etServerUrl.getText().toString().trim();

        if (!"gateway".equals(type) && botToken.isEmpty()) {
            showTestResult(false, "Bot Token을 입력하세요");
            return;
        }
        if ("gateway".equals(type) && serverUrl.isEmpty()) {
            showTestResult(false, "Server URL을 입력하세요");
            return;
        }

        binding.btnTestConnection.setEnabled(false);
        binding.tvTestResult.setVisibility(View.VISIBLE);
        binding.tvTestResult.setTextColor(Color.GRAY);
        binding.tvTestResult.setText("연결 테스트 중...");

        Single<String> task = Single.fromCallable(() -> {
            switch (type) {
                case "telegram":
                    return httpGet("https://api.telegram.org/bot" + botToken + "/getMe", null);
                case "discord":
                    return httpGet("https://discord.com/api/v10/users/@me", "Bot " + botToken);
                case "slack":
                    return httpGet("https://slack.com/api/auth.test", "Bearer " + botToken);
                case "gateway":
                default:
                    return httpGet(serverUrl, botToken.isEmpty() ? null : "Bearer " + botToken);
            }
        });

        disposables.add(task
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        msg -> {
                            if (binding == null) return;
                            binding.btnTestConnection.setEnabled(true);
                            showTestResult(true, "연결 성공 · " + msg);
                            binding.btnSave.setEnabled(true);
                        },
                        err -> {
                            if (binding == null) return;
                            binding.btnTestConnection.setEnabled(true);
                            showTestResult(false, "연결 실패: " + err.getMessage());
                        }));
    }

    private String httpGet(String url, @Nullable String authHeader) throws IOException {
        Request.Builder rb = new Request.Builder().url(url).get();
        if (authHeader != null) rb.addHeader("Authorization", authHeader);
        try (Response resp = http.newCall(rb.build()).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("HTTP " + resp.code());
            }
            return "HTTP " + resp.code();
        }
    }

    private void showTestResult(boolean ok, String msg) {
        binding.tvTestResult.setVisibility(View.VISIBLE);
        binding.tvTestResult.setTextColor(ok ? 0xFF2E7D32 : 0xFFC62828);
        binding.tvTestResult.setText(msg);
    }

    private void saveChannel(View view) {
        String name = binding.etChannelName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.etChannelName.setError("이름을 입력하세요");
            return;
        }

        String type = currentType();

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
        entity.setStatus("connected");
        String now = Instant.now().toString();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        disposables.add(
                channelDao.insert(entity)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Toast.makeText(getContext(), "채널이 추가되었습니다", Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(view).popBackStack();
                                },
                                e -> Toast.makeText(getContext(), "오류: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
