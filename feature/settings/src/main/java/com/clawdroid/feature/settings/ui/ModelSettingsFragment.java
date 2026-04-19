package com.clawdroid.feature.settings.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.feature.settings.databinding.FragmentModelSettingsBinding;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ModelSettingsFragment extends Fragment {

    private FragmentModelSettingsBinding binding;

    @Inject
    SettingsRepository settingsRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModelSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        setupProviderCards();
    }

    private void setupProviderCards() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 16, 24, 16);

        // OpenAI
        container.addView(createProviderCard("OpenAI", "GPT-4o, GPT-4o Mini",
                () -> showApiKeyDialog("OpenAI API Key", "openai_api_key")));

        // Gemini Cloud
        container.addView(createProviderCard("Gemini Cloud", "Gemini 2.5 Flash / Pro",
                () -> showApiKeyDialog("Gemini API Key", "gemini_api_key")));

        // Gemini Nano
        container.addView(createProviderCard("Gemini Nano", "온디바이스 (API 키 불필요)", null));

        // Ollama
        container.addView(createProviderCard("Ollama", "로컬 서버",
                () -> showOllamaDialog()));

        // Custom Endpoint
        container.addView(createProviderCard("Custom", "OpenAI 호환 엔드포인트",
                () -> showCustomEndpointDialog()));

        // Model Parameters section
        container.addView(createSectionHeader("모델 파라미터"));
        container.addView(createSlider("Temperature", 0f, 2f,
                settingsRepository.getTemperature(),
                v -> settingsRepository.setTemperature(v)));
        container.addView(createSlider("Top P", 0f, 1f,
                settingsRepository.getTopP(),
                v -> settingsRepository.setTopP(v)));
        container.addView(createSlider("Max Tokens", 256, 8192,
                settingsRepository.getMaxTokens(),
                v -> settingsRepository.setMaxTokens((int) v)));

        // TTS section
        container.addView(createSectionHeader("TTS 설정"));
        container.addView(createProviderCard("ElevenLabs", "고품질 음성 합성",
                () -> showApiKeyDialog("ElevenLabs API Key", "elevenlabs_api_key")));

        // Wrap in ScrollView-like behavior via RecyclerView replacement
        binding.contentContainer.addView(container);
    }

    private View createProviderCard(String name, String desc, @Nullable Runnable onConfigure) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 8);
        card.setLayoutParams(lp);

        TextView tvName = new TextView(requireContext());
        tvName.setText(name);
        tvName.setTextSize(16);
        tvName.setTextColor(0xFF000000);
        card.addView(tvName);

        TextView tvDesc = new TextView(requireContext());
        tvDesc.setText(desc);
        tvDesc.setTextSize(12);
        tvDesc.setTextColor(0xFF888888);
        card.addView(tvDesc);

        if (onConfigure != null) {
            card.setOnClickListener(v -> onConfigure.run());
        }

        return card;
    }

    private View createSectionHeader(String title) {
        TextView tv = new TextView(requireContext());
        tv.setText(title);
        tv.setTextSize(14);
        tv.setTextColor(0xFF6200EE);
        tv.setPadding(0, 32, 0, 8);
        return tv;
    }

    private View createSlider(String label, float min, float max, float current,
                              SliderCallback callback) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label + ": " + String.format("%.2f", current));
        row.addView(tvLabel);

        Slider slider = new Slider(requireContext());
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setValue(Math.min(Math.max(current, min), max));
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                tvLabel.setText(label + ": " + String.format("%.2f", value));
                callback.onChanged(value);
            }
        });
        row.addView(slider);

        return row;
    }

    private void showApiKeyDialog(String title, String prefKey) {
        EditText input = new EditText(requireContext());
        input.setHint("API Key 입력...");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(input)
                .setPositiveButton("저장", (d, w) -> {
                    String key = input.getText().toString().trim();
                    if (!key.isEmpty()) {
                        settingsRepository.saveApiKey(prefKey, key);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showOllamaDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("http://localhost:11434");
        input.setText(settingsRepository.getOllamaEndpoint());
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(requireContext())
                .setTitle("Ollama 서버 설정")
                .setView(input)
                .setPositiveButton("저장", (d, w) -> {
                    String endpoint = input.getText().toString().trim();
                    if (!endpoint.isEmpty()) {
                        settingsRepository.setOllamaEndpoint(endpoint);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showCustomEndpointDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etUrl = new EditText(requireContext());
        etUrl.setHint("엔드포인트 URL");
        layout.addView(etUrl);

        EditText etKey = new EditText(requireContext());
        etKey.setHint("API Key (선택)");
        layout.addView(etKey);

        new AlertDialog.Builder(requireContext())
                .setTitle("Custom 엔드포인트 설정")
                .setView(layout)
                .setPositiveButton("저장", (d, w) -> {
                    settingsRepository.saveApiKey("custom_endpoint", etUrl.getText().toString().trim());
                    settingsRepository.saveApiKey("custom_api_key", etKey.getText().toString().trim());
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface SliderCallback {
        void onChanged(float value);
    }
}
