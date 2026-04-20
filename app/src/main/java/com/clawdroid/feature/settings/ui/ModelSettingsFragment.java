package com.clawdroid.feature.settings.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.core.ai.AiProvider;
import com.clawdroid.core.ai.AiProviderManager;
import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.core.model.ModelInfo;
import com.clawdroid.app.R;
import com.clawdroid.app.databinding.FragmentModelSettingsBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class ModelSettingsFragment extends Fragment {

    private FragmentModelSettingsBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final List<String> connectedProviderIds = new ArrayList<>();

    @Inject
    SettingsRepository settingsRepository;

    @Inject
    AiProviderManager providerManager;

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

        binding.btnAddApi.setOnClickListener(v -> showAddApiDialog());
        binding.btnAddModel.setOnClickListener(v -> showAddModelDialog());

        setupFallback();
        setupSliders();
        refreshConnectedApis();
    }

    private void refreshConnectedApis() {
        connectedProviderIds.clear();
        binding.connectedApiContainer.removeAllViews();

        // Gemini Nano (온디바이스) — API 키 불필요, 항상 기본 제공
        connectedProviderIds.add("gemini-nano");

        List<String> allIds = providerManager.getAllProviderIds();
        List<Single<Boolean>> checks = new ArrayList<>();
        for (String id : allIds) {
            if ("gemini-nano".equals(id)) continue; // 이미 기본 추가됨
            AiProvider provider = providerManager.getProvider(id);
            if (provider != null) {
                checks.add(provider.isAvailable().map(avail -> {
                    if (avail) {
                        synchronized (connectedProviderIds) {
                            connectedProviderIds.add(id);
                        }
                    }
                    return avail;
                }));
            }
        }

        if (checks.isEmpty()) {
            updateModelSectionState();
            return;
        }

        disposables.add(
                Single.zip(checks, results -> results)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(results -> {
                            populateConnectedApis();
                            updateModelSectionState();
                        }, err -> updateModelSectionState())
        );
    }

    private void populateConnectedApis() {
        binding.connectedApiContainer.removeAllViews();

        if (connectedProviderIds.isEmpty()) {
            binding.tvNoApiConnected.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvNoApiConnected.setVisibility(View.GONE);

        for (String id : connectedProviderIds) {
            AiProvider provider = providerManager.getProvider(id);
            if (provider == null) continue;

            float dp = getResources().getDisplayMetrics().density;

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (8 * dp);
            card.setLayoutParams(lp);
            card.setCardElevation(0);
            card.setStrokeWidth((int) dp);
            card.setStrokeColor(getResources().getColor(R.color.md_outline_variant, null));
            card.setRadius(12 * dp);
            card.setCardBackgroundColor(getResources().getColor(R.color.md_surface, null));

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = (int) (16 * dp);
            inner.setPadding(pad, pad, pad, pad);

            TextView tvName = new TextView(requireContext());
            tvName.setText(provider.getName());
            tvName.setTextSize(16);
            tvName.setTextColor(getResources().getColor(R.color.md_on_surface, null));
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(tvLp);

            TextView tvStatus = new TextView(requireContext());
            // Gemini Nano는 온디바이스 — "기기 내장" 표시
            if ("gemini-nano".equals(id)) {
                tvStatus.setText("✓ 기기 내장");
            } else {
                tvStatus.setText("✓ 연결됨");
            }
            tvStatus.setTextSize(13);
            tvStatus.setTextColor(getResources().getColor(R.color.md_primary, null));

            inner.addView(tvName);
            inner.addView(tvStatus);
            card.addView(inner);
            binding.connectedApiContainer.addView(card);
        }
    }

    private void updateModelSectionState() {
        boolean hasApi = !connectedProviderIds.isEmpty();
        binding.btnAddModel.setEnabled(hasApi);
        binding.btnAddModel.setAlpha(hasApi ? 1f : 0.5f);
        binding.tvModelDisabled.setVisibility(hasApi ? View.GONE : View.VISIBLE);
        binding.modelListContainer.setVisibility(hasApi ? View.VISIBLE : View.GONE);

        if (hasApi) {
            loadSelectedModel();
        }
    }

    private void loadSelectedModel() {
        String activeProvider = settingsRepository.getActiveProvider();
        if (activeProvider == null || activeProvider.isEmpty()) return;

        binding.modelListContainer.removeAllViews();
        AiProvider provider = providerManager.getProvider(activeProvider);
        if (provider == null) return;

        String savedModelId = settingsRepository.getDefaultModelId(activeProvider);
        String subtitle = (savedModelId != null && !savedModelId.isEmpty())
                ? savedModelId
                : "기본 모델";
        MaterialCardView card = createModelCard(provider.getName(), subtitle, true);
        binding.modelListContainer.addView(card);
    }

    private void showAddModelDialog() {
        if (connectedProviderIds.isEmpty()) {
            Toast.makeText(requireContext(), "API\ub97c \uba3c\uc800 \uc5f0\uacb0\ud574\uc8fc\uc138\uc694.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Single<List<ModelInfo>>> modelQueries = new ArrayList<>();
        List<String> providerLabels = new ArrayList<>();

        for (String id : connectedProviderIds) {
            AiProvider provider = providerManager.getProvider(id);
            if (provider != null) {
                modelQueries.add(provider.listModels());
                providerLabels.add(provider.getName());
            }
        }

        disposables.add(
                Single.zip(modelQueries, results -> {
                    List<String[]> items = new ArrayList<>();
                    for (int i = 0; i < results.length; i++) {
                        @SuppressWarnings("unchecked")
                        List<ModelInfo> models = (List<ModelInfo>) results[i];
                        String provName = providerLabels.get(i);
                        String provId = connectedProviderIds.get(i);
                        for (ModelInfo m : models) {
                            // items[n] = {displayName, providerId, modelId}
                            items.add(new String[]{provName + " - " + m.getName(), provId, m.getId()});
                        }
                    }
                    return items;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(items -> {
                            if (items.isEmpty()) {
                                Toast.makeText(requireContext(), "사용 가능한 모델이 없습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String[] names = new String[items.size()];
                            for (int i = 0; i < items.size(); i++) {
                                names[i] = items.get(i)[0];
                            }
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("모델 선택")
                                    .setItems(names, (dlg, which) -> {
                                        String selectedProvider = items.get(which)[1];
                                        String selectedModelId = items.get(which)[2];
                                        settingsRepository.setActiveProvider(selectedProvider);
                                        settingsRepository.setDefaultModelId(selectedProvider, selectedModelId);
                                        loadSelectedModel();
                                        Toast.makeText(requireContext(), names[which] + " 선택됨", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("취소", null)
                                    .show();
                        }, err -> Toast.makeText(requireContext(), "모델 목록 로드 실패", Toast.LENGTH_SHORT).show())
        );
    }

    private MaterialCardView createModelCard(String name, String subtitle, boolean active) {
        float dp = getResources().getDisplayMetrics().density;

        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * dp);
        card.setLayoutParams(lp);
        card.setCardElevation(0);
        card.setStrokeWidth((int) dp);
        card.setStrokeColor(getResources().getColor(
                active ? R.color.md_primary : R.color.md_outline_variant, null));
        card.setRadius(12 * dp);
        card.setCardBackgroundColor(getResources().getColor(R.color.md_surface, null));

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * dp);
        inner.setPadding(pad, pad, pad, pad);

        TextView tvName = new TextView(requireContext());
        tvName.setText(name);
        tvName.setTextSize(15);
        tvName.setTextColor(getResources().getColor(R.color.md_on_surface, null));

        TextView tvSub = new TextView(requireContext());
        tvSub.setText(subtitle);
        tvSub.setTextSize(13);
        tvSub.setTextColor(getResources().getColor(R.color.md_on_surface_variant, null));

        inner.addView(tvName);
        inner.addView(tvSub);
        card.addView(inner);
        return card;
    }

    private void showAddApiDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_api, null);

        LinearLayout stepProvider = dialogView.findViewById(R.id.stepSelectProvider);
        LinearLayout stepApiKey = dialogView.findViewById(R.id.stepApiKey);
        LinearLayout stepResult = dialogView.findViewById(R.id.stepTestResult);
        RadioGroup rgProvider = dialogView.findViewById(R.id.rgProvider);
        TextInputEditText etApiKey = dialogView.findViewById(R.id.etDialogApiKey);
        TextInputEditText etEndpoint = dialogView.findViewById(R.id.etDialogEndpoint);
        View tilEndpoint = dialogView.findViewById(R.id.tilDialogEndpoint);
        TextView tvTitle = dialogView.findViewById(R.id.tvApiDialogTitle);
        View progressTest = dialogView.findViewById(R.id.progressTest);
        TextView tvTestStatus = dialogView.findViewById(R.id.tvTestStatus);
        TextView tvTestDetail = dialogView.findViewById(R.id.tvTestDetail);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("\ub2e4\uc74c", null)
                .setNegativeButton("\ucde8\uc18c", null)
                .create();

        dialog.show();

        final String[] selectedProviderId = {null};
        final int[] currentStep = {1};

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (currentStep[0] == 1) {
                int checkedId = rgProvider.getCheckedRadioButtonId();
                if (checkedId == R.id.rbProviderOpenAi) {
                    selectedProviderId[0] = "openai";
                    tvTitle.setText("OpenAI API Key \uc785\ub825");
                    tilEndpoint.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbProviderGemini) {
                    selectedProviderId[0] = "gemini-cloud";
                    tvTitle.setText("Gemini API Key \uc785\ub825");
                    tilEndpoint.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbProviderOllama) {
                    selectedProviderId[0] = "ollama";
                    tvTitle.setText("Ollama \uc124\uc815");
                    tilEndpoint.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.rbProviderCustom) {
                    selectedProviderId[0] = "custom";
                    tvTitle.setText("Custom API \uc124\uc815");
                    tilEndpoint.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(requireContext(), "\ud504\ub85c\ubc14\uc774\ub354\ub97c \uc120\ud0dd\ud574\uc8fc\uc138\uc694.", Toast.LENGTH_SHORT).show();
                    return;
                }

                stepProvider.setVisibility(View.GONE);
                stepApiKey.setVisibility(View.VISIBLE);
                currentStep[0] = 2;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("\uc5f0\uacb0 \ud14c\uc2a4\ud2b8");

            } else if (currentStep[0] == 2) {
                String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
                if (apiKey.isEmpty() && !"ollama".equals(selectedProviderId[0])) {
                    Toast.makeText(requireContext(), "API Key\ub97c \uc785\ub825\ud574\uc8fc\uc138\uc694.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!apiKey.isEmpty()) {
                    settingsRepository.saveApiKey(selectedProviderId[0], apiKey);
                }

                if (tilEndpoint.getVisibility() == View.VISIBLE) {
                    String endpoint = etEndpoint.getText() != null ? etEndpoint.getText().toString().trim() : "";
                    if (!endpoint.isEmpty()) {
                        settingsRepository.setOllamaEndpoint(endpoint);
                    }
                }

                stepApiKey.setVisibility(View.GONE);
                stepResult.setVisibility(View.VISIBLE);
                progressTest.setVisibility(View.VISIBLE);
                tvTestStatus.setText("\uc5f0\uacb0 \ud14c\uc2a4\ud2b8 \uc911...");
                tvTestDetail.setText("");
                currentStep[0] = 3;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("\uc644\ub8cc");

                AiProvider provider = providerManager.getProvider(selectedProviderId[0]);
                if (provider != null) {
                    disposables.add(
                            provider.isAvailable()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(available -> {
                                        progressTest.setVisibility(View.GONE);
                                        if (available) {
                                            tvTestStatus.setText("\u2713 \uc5f0\uacb0 \uc131\uacf5!");
                                            tvTestDetail.setText(provider.getName() + " \uc5f0\uacb0\uc774 \ud655\uc778\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                        } else {
                                            tvTestStatus.setText("\u2717 \uc5f0\uacb0 \uc2e4\ud328");
                                            tvTestDetail.setText("API \ud0a4 \ub610\ub294 \uc5d4\ub4dc\ud3ec\uc778\ud2b8\ub97c \ud655\uc778\ud574\uc8fc\uc138\uc694.");
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                        }
                                    }, err -> {
                                        progressTest.setVisibility(View.GONE);
                                        tvTestStatus.setText("\u2717 \uc5f0\uacb0 \uc2e4\ud328");
                                        tvTestDetail.setText(err.getMessage() != null ? err.getMessage() : "\uc54c \uc218 \uc5c6\ub294 \uc624\ub958");
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    })
                    );
                }

            } else if (currentStep[0] == 3) {
                dialog.dismiss();
                refreshConnectedApis();
            }
        });
    }

    private void setupFallback() {
        binding.switchFallback.setChecked(settingsRepository.isFallbackEnabled());
        binding.switchFallback.setOnCheckedChangeListener((btn, checked) ->
                settingsRepository.setFallbackEnabled(checked));
    }

    private void setupSliders() {
        float temp = settingsRepository.getTemperature();
        float topP = settingsRepository.getTopP();
        int maxTokens = settingsRepository.getMaxTokens();

        binding.sliderTemperature.setValue(snapToStep(Math.max(0f, Math.min(2f, temp)), 0.05f));
        binding.tvTemperature.setText(String.format("Temperature: %.2f", temp));
        binding.sliderTemperature.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                binding.tvTemperature.setText(String.format("Temperature: %.2f", value));
                settingsRepository.setTemperature(value);
            }
        });

        binding.sliderTopP.setValue(snapToStep(Math.max(0f, Math.min(1f, topP)), 0.05f));
        binding.tvTopP.setText(String.format("Top P: %.2f", topP));
        binding.sliderTopP.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                binding.tvTopP.setText(String.format("Top P: %.2f", value));
                settingsRepository.setTopP(value);
            }
        });

        float tokenVal = snapToStep(Math.max(256, Math.min(8192, maxTokens)), 256f);
        binding.sliderMaxTokens.setValue(tokenVal);
        binding.tvMaxTokens.setText(String.format("Max Tokens: %d", maxTokens));
        binding.sliderMaxTokens.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                binding.tvMaxTokens.setText(String.format("Max Tokens: %d", (int) value));
                settingsRepository.setMaxTokens((int) value);
            }
        });
    }

    private float snapToStep(float value, float step) {
        return Math.round(value / step) * step;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}
