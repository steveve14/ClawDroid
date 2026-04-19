package com.clawdroid.core.ui.markdown;

import android.content.Context;
import android.widget.TextView;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;

public final class MarkdownRenderer {

    private static volatile Markwon instance;

    private MarkdownRenderer() {}

    public static Markwon getInstance(Context context) {
        if (instance == null) {
            synchronized (MarkdownRenderer.class) {
                if (instance == null) {
                    instance = Markwon.builder(context.getApplicationContext())
                            .usePlugin(HtmlPlugin.create())
                            .usePlugin(TablePlugin.create(context.getApplicationContext()))
                            .build();
                }
            }
        }
        return instance;
    }

    public static void render(TextView textView, String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            textView.setText("");
            return;
        }
        getInstance(textView.getContext()).setMarkdown(textView, markdown);
    }
}
