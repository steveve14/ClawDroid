package com.clawdroid.feature.settings.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.app.databinding.FragmentAboutBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        // Set version dynamically
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            binding.tvVersion.setText("v" + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvVersion.setText("v1.0.0");
        }

        binding.btnTerms.setOnClickListener(v ->
                showInfoDialog("이용약관",
                        "ClawDroid는 개인용 AI 어시스턴트 앱입니다.\n\n"
                        + "본 앱은 사용자의 데이터를 기기 내에서 처리하며, "
                        + "클라우드 AI 모델 사용 시에만 서버와 통신합니다.\n\n"
                        + "사용자는 본 앱을 통해 생성된 AI 응답의 정확성을 "
                        + "직접 확인할 책임이 있습니다."));

        binding.btnPrivacy.setOnClickListener(v ->
                showInfoDialog("개인정보처리방침",
                        "ClawDroid는 사용자의 개인정보를 소중히 다룹니다.\n\n"
                        + "• 대화 데이터: 기기 내 암호화 저장\n"
                        + "• API 키: EncryptedSharedPreferences로 안전하게 보관\n"
                        + "• 클라우드 AI 사용 시: 대화 내용이 서버로 전송될 수 있음\n"
                        + "• 온디바이스 AI: 네트워크 없이 기기 내에서 처리\n"
                        + "• 채널 데이터: 연결된 채널의 메시지만 처리\n\n"
                        + "사용자는 언제든지 모든 데이터를 삭제할 수 있습니다."));

        binding.btnLicenses.setOnClickListener(v ->
                showInfoDialog("오픈소스 라이선스",
                        "본 앱은 다음 오픈소스 라이브러리를 사용합니다:\n\n"
                        + "• AndroidX (Apache 2.0)\n"
                        + "• Material Components (Apache 2.0)\n"
                        + "• Room Database (Apache 2.0)\n"
                        + "• Hilt / Dagger (Apache 2.0)\n"
                        + "• RxJava 3 (Apache 2.0)\n"
                        + "• Retrofit / OkHttp (Apache 2.0)\n"
                        + "• Glide (BSD / Apache 2.0)\n"
                        + "• Markwon (Apache 2.0)\n"
                        + "• Jsoup (MIT)\n"
                        + "• Gson (Apache 2.0)"));
    }

    private void showInfoDialog(String title, String content) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("확인", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
