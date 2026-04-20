package com.clawdroid.feature.tools.builtin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Single;

public class LocationTool implements Tool {

    private final Context context;

    @Inject
    public LocationTool(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override public String getName() { return "location"; }

    @Override
    public String getDescription() {
        return "현재 기기의 위치 정보(위도, 경도, 주소)를 조회합니다.";
    }

    @Override
    public List<String> getRequiredPermissions() {
        return Collections.singletonList(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject detail = new JsonObject();
        detail.addProperty("type", "boolean");
        detail.addProperty("description", "true일 경우 주소까지 포함하여 조회");
        properties.add("include_address", detail);

        params.add("properties", properties);
        return params;
    }

    @SuppressLint("MissingPermission")
    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            try {
                LocationManager lm = (LocationManager)
                        context.getSystemService(Context.LOCATION_SERVICE);
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (location == null) {
                    return new ToolResult("location", false, "위치를 가져올 수 없습니다.");
                }

                double lat = location.getLatitude();
                double lon = location.getLongitude();
                StringBuilder sb = new StringBuilder();
                sb.append("위도: ").append(lat).append("\n");
                sb.append("경도: ").append(lon).append("\n");

                boolean includeAddress = params.has("include_address")
                        && params.get("include_address").getAsBoolean();
                if (includeAddress) {
                    try {
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address addr = addresses.get(0);
                            sb.append("주소: ").append(addr.getAddressLine(0));
                        }
                    } catch (Exception e) {
                        sb.append("주소 변환 실패: ").append(e.getMessage());
                    }
                }

                return new ToolResult("location", true, sb.toString());
            } catch (Exception e) {
                return new ToolResult("location", false, "위치 조회 오류: " + e.getMessage());
            }
        });
    }
}
