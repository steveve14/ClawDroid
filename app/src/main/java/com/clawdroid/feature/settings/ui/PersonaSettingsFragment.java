package com.clawdroid.feature.settings.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.core.data.db.entity.PersonaEntity;
import com.clawdroid.core.data.repository.PersonaRepository;
import com.clawdroid.app.databinding.FragmentPersonaSettingsBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class PersonaSettingsFragment extends Fragment {

    private FragmentPersonaSettingsBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private static final String[] STYLES = {"Professional", "Friendly", "Humorous", "Concise"};

    @Inject
    PersonaRepository personaRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPersonaSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.fabAddPersona.setOnClickListener(v -> showEditDialog(null));

        loadPersonas();
    }

    private void loadPersonas() {
        disposables.add(
            personaRepository.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::renderPersonaList, e -> {})
        );
    }

    private void renderPersonaList(List<PersonaEntity> personas) {
        LinearLayout container = binding.personaListContainer;
        container.removeAllViews();

        if (personas.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvEmpty.setVisibility(View.GONE);

        float dp = getResources().getDisplayMetrics().density;

        for (PersonaEntity persona : personas) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (10 * dp);
            card.setLayoutParams(lp);
            card.setCardElevation(0);
            card.setRadius(16 * dp);
            card.setStrokeWidth((int) dp);
            card.setStrokeColor(getResources().getColor(
                    persona.isActive()
                            ? com.clawdroid.app.R.color.md_primary
                            : com.clawdroid.app.R.color.md_outline_variant, null));
            card.setCardBackgroundColor(getResources().getColor(
                    persona.isActive()
                            ? com.clawdroid.app.R.color.md_primary_container
                            : com.clawdroid.app.R.color.md_surface_container_low, null));

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = (int) (16 * dp);
            inner.setPadding(pad, pad, pad, pad);

            LinearLayout textGroup = new LinearLayout(requireContext());
            textGroup.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textGroup.setLayoutParams(textLp);

            TextView tvName = new TextView(requireContext());
            tvName.setText(persona.getName());
            tvName.setTextSize(16);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setTextColor(getResources().getColor(com.clawdroid.app.R.color.md_on_surface, null));

            TextView tvStyle = new TextView(requireContext());
            String style = persona.getConversationStyle() != null ? persona.getConversationStyle() : "-";
            tvStyle.setText(style);
            tvStyle.setTextSize(13);
            tvStyle.setTextColor(getResources().getColor(com.clawdroid.app.R.color.md_on_surface_variant, null));

            textGroup.addView(tvName);
            textGroup.addView(tvStyle);

            if (persona.isActive()) {
                TextView tvActive = new TextView(requireContext());
                tvActive.setText("\ud65c\uc131");
                tvActive.setTextSize(12);
                tvActive.setTextColor(getResources().getColor(com.clawdroid.app.R.color.md_primary, null));
                LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                badgeLp.setMarginEnd((int) (8 * dp));
                tvActive.setLayoutParams(badgeLp);
                inner.addView(tvActive);
            }

            inner.addView(textGroup);

            TextView btnEdit = new TextView(requireContext());
            btnEdit.setText("\ud3b8\uc9d1");
            btnEdit.setTextSize(13);
            btnEdit.setTextColor(getResources().getColor(com.clawdroid.app.R.color.md_primary, null));
            btnEdit.setPadding(pad / 2, pad / 2, pad / 2, pad / 2);
            btnEdit.setOnClickListener(v -> showEditDialog(persona));
            inner.addView(btnEdit);

            TextView btnDelete = new TextView(requireContext());
            btnDelete.setText("\uc0ad\uc81c");
            btnDelete.setTextSize(13);
            btnDelete.setTextColor(getResources().getColor(com.clawdroid.app.R.color.md_error, null));
            btnDelete.setPadding(pad / 2, pad / 2, pad / 2, pad / 2);
            btnDelete.setOnClickListener(v -> confirmDelete(persona));
            inner.addView(btnDelete);

            card.addView(inner);

            card.setOnClickListener(v -> {
                if (!persona.isActive()) {
                    disposables.add(
                        personaRepository.setActive(persona.getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                () -> Snackbar.make(binding.getRoot(), persona.getName() + " \ud65c\uc131\ud654\ub428", Snackbar.LENGTH_SHORT).show(),
                                e -> {}
                            )
                    );
                }
            });

            container.addView(card);
        }
    }

    private void showEditDialog(@Nullable PersonaEntity existing) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        TextInputLayout tilName = new TextInputLayout(requireContext(),
                null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilName.setHint("페르소나 이름");
        TextInputEditText etName = new TextInputEditText(requireContext());
        if (existing != null) etName.setText(existing.getName());
        tilName.addView(etName);

        TextInputLayout tilPrompt = new TextInputLayout(requireContext(),
                null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilPrompt.setHint("System Prompt");
        tilPrompt.setCounterEnabled(true);
        tilPrompt.setCounterMaxLength(2000);
        TextInputEditText etPrompt = new TextInputEditText(requireContext());
        etPrompt.setMinLines(4);
        etPrompt.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etPrompt.setGravity(android.view.Gravity.TOP);
        if (existing != null && existing.getSystemPrompt() != null) {
            etPrompt.setText(existing.getSystemPrompt());
        }
        tilPrompt.addView(etPrompt);

        TextInputLayout tilStyle = new TextInputLayout(requireContext(),
                null, com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        tilStyle.setHint("\ub300\ud654 \uc2a4\ud0c0\uc77c");
        android.widget.AutoCompleteTextView actvStyle = new android.widget.AutoCompleteTextView(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, STYLES);
        actvStyle.setAdapter(adapter);
        actvStyle.setInputType(android.text.InputType.TYPE_NULL);
        String currentStyle = (existing != null && existing.getConversationStyle() != null)
                ? existing.getConversationStyle() : "Professional";
        actvStyle.setText(currentStyle, false);
        tilStyle.addView(actvStyle);

        LinearLayout.LayoutParams marginLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        marginLp.topMargin = pad;
        tilPrompt.setLayoutParams(marginLp);

        LinearLayout.LayoutParams marginLp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        marginLp2.topMargin = pad;
        tilStyle.setLayoutParams(marginLp2);

        layout.addView(tilName);
        layout.addView(tilPrompt);
        layout.addView(tilStyle);

        String title = existing == null ? "\ud398\ub974\uc18c\ub098 \ucd94\uac00" : "\ud398\ub974\uc18c\ub098 \ud3b8\uc9d1";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("\uc800\uc7a5", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        Snackbar.make(binding.getRoot(), "\uc774\ub984\uc744 \uc785\ub825\ud574 \uc8fc\uc138\uc694", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    String prompt = etPrompt.getText() != null ? etPrompt.getText().toString() : "";
                    String style = actvStyle.getText().toString();

                    PersonaEntity entity = existing != null ? existing : new PersonaEntity();
                    entity.setName(name);
                    entity.setSystemPrompt(prompt);
                    entity.setConversationStyle(style);

                    disposables.add(
                        (existing == null
                                ? personaRepository.save(entity)
                                : personaRepository.update(entity))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                () -> Snackbar.make(binding.getRoot(), "\uc800\uc7a5\ub418\uc5c8\uc2b5\ub2c8\ub2e4", Snackbar.LENGTH_SHORT).show(),
                                e -> Snackbar.make(binding.getRoot(), "\uc800\uc7a5 \uc2e4\ud328", Snackbar.LENGTH_SHORT).show()
                            )
                    );
                })
                .setNegativeButton("\ucde8\uc18c", null)
                .show();
    }

    private void confirmDelete(PersonaEntity persona) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("\ud398\ub974\uc18c\ub098 \uc0ad\uc81c")
                .setMessage("\"" + persona.getName() + "\"\uc744(\ub97c) \uc0ad\uc81c\ud560\uae4c\uc694?")
                .setPositiveButton("\uc0ad\uc81c", (dialog, which) -> {
                    disposables.add(
                        personaRepository.delete(persona.getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                () -> Snackbar.make(binding.getRoot(), "\uc0ad\uc81c\ub418\uc5c8\uc2b5\ub2c8\ub2e4", Snackbar.LENGTH_SHORT).show(),
                                e -> {}
                            )
                    );
                })
                .setNegativeButton("\ucde8\uc18c", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}