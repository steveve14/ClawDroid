package com.clawdroid.feature.tools.skill;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class BuiltinSkillInstaller {

    private static final String ASSETS_SKILLS_DIR = "skills";
    private static final String[] BUILTIN_SKILLS = {"weather", "news", "translate"};

    private final Context context;

    @Inject
    public BuiltinSkillInstaller(@ApplicationContext Context context) {
        this.context = context;
    }

    public void installIfNeeded() {
        File skillsDir = new File(context.getFilesDir(), ASSETS_SKILLS_DIR);
        if (!skillsDir.exists()) {
            skillsDir.mkdirs();
        }

        for (String skillName : BUILTIN_SKILLS) {
            File skillDir = new File(skillsDir, skillName);
            File skillFile = new File(skillDir, "SKILL.md");
            if (!skillFile.exists()) {
                installSkill(skillName, skillDir, skillFile);
            }
        }
    }

    private void installSkill(String skillName, File skillDir, File skillFile) {
        if (!skillDir.exists()) {
            skillDir.mkdirs();
        }

        String assetPath = ASSETS_SKILLS_DIR + "/" + skillName + "/SKILL.md";
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(skillFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            // Built-in skill not found in assets — skip silently
        }
    }
}
