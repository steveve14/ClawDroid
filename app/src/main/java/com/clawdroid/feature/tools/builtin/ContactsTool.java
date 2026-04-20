package com.clawdroid.feature.tools.builtin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Single;

public class ContactsTool implements Tool {

    private final Context context;

    @Inject
    public ContactsTool(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override public String getName() { return "contacts"; }

    @Override
    public String getDescription() {
        return "연락처에서 사람을 검색하거나 연락처 정보를 조회합니다.";
    }

    @Override
    public List<String> getRequiredPermissions() {
        return Collections.singletonList(Manifest.permission.READ_CONTACTS);
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject query = new JsonObject();
        query.addProperty("type", "string");
        query.addProperty("description", "검색할 이름 또는 전화번호");
        properties.add("query", query);

        params.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("query");
        params.add("required", required);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            String query = params.has("query") ? params.get("query").getAsString() : null;
            if (query == null || query.isEmpty()) {
                return new ToolResult("contacts", false, "query 파라미터가 필요합니다.");
            }
            return searchContacts(query);
        });
    }

    private ToolResult searchContacts(String query) {
        try {
            ContentResolver cr = context.getContentResolver();
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {"%" + query + "%"};
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            Cursor cursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection, selection, selectionArgs,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            StringBuilder sb = new StringBuilder();
            int count = 0;
            if (cursor != null) {
                while (cursor.moveToNext() && count < 10) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    sb.append(++count).append(". ").append(name)
                            .append(" - ").append(number).append("\n");
                }
                cursor.close();
            }

            return new ToolResult("contacts", true,
                    count > 0 ? sb.toString() : "'" + query + "'에 대한 연락처를 찾을 수 없습니다.");
        } catch (Exception e) {
            return new ToolResult("contacts", false, "연락처 검색 오류: " + e.getMessage());
        }
    }
}
