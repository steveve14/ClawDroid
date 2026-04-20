package com.clawdroid.feature.tools.builtin;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BrowserTool implements Tool {

    private final OkHttpClient httpClient;

    @Inject
    public BrowserTool(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override public String getName() { return "browser"; }

    @Override
    public String getDescription() {
        return "웹 페이지를 검색하거나 URL의 내용을 요약합니다.";
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject query = new JsonObject();
        query.addProperty("type", "string");
        query.addProperty("description", "검색할 키워드 또는 URL");
        properties.add("query", query);

        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("search");
        enumValues.add("fetch");
        action.add("enum", enumValues);
        action.addProperty("description", "search: 웹 검색, fetch: URL 내용 가져오기");
        properties.add("action", action);

        params.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("query");
        required.add("action");
        params.add("required", required);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            String queryStr = params.has("query") ? params.get("query").getAsString() : null;
            if (queryStr == null || queryStr.isEmpty()) {
                return new ToolResult("browser", false, "query 파라미터가 필요합니다.");
            }

            String actionStr = params.has("action") ? params.get("action").getAsString() : "search";
            switch (actionStr) {
                case "fetch": return fetchUrl(queryStr);
                case "search":
                default: return webSearch(queryStr);
            }
        });
    }

    private ToolResult webSearch(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            Request request = new Request.Builder()
                    .url(searchUrl)
                    .addHeader("User-Agent", "ClawDroid/1.0")
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                String html = response.body().string();
                Document doc = Jsoup.parse(html);
                Elements results = doc.select(".result__body");

                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (Element result : results) {
                    if (count >= 5) break;
                    String title = result.select(".result__title").text();
                    String snippet = result.select(".result__snippet").text();
                    String link = result.select(".result__url").text();
                    sb.append(++count).append(". ").append(title).append("\n");
                    sb.append("   ").append(snippet).append("\n");
                    sb.append("   URL: ").append(link).append("\n\n");
                }
                return new ToolResult("browser", true,
                        sb.length() > 0 ? sb.toString() : "검색 결과 없음");
            }
        } catch (Exception e) {
            return new ToolResult("browser", false, "검색 오류: " + e.getMessage());
        }
    }

    private ToolResult fetchUrl(String url) {
        try {
            if (!url.startsWith("http")) url = "https://" + url;

            // SSRF protection: block private/internal network and non-HTTP schemes
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                return new ToolResult("browser", false, "HTTP/HTTPS URL만 지원합니다.");
            }
            String host = uri.getHost();
            if (host == null || isPrivateHost(host)) {
                return new ToolResult("browser", false, "내부 네트워크 주소는 접근할 수 없습니다.");
            }

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "ClawDroid/1.0")
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                String html = response.body().string();
                Document doc = Jsoup.parse(html);
                doc.select("script, style, nav, footer, header").remove();
                String text = doc.body().text();
                if (text.length() > 3000) text = text.substring(0, 3000) + "...";
                return new ToolResult("browser", true, text);
            }
        } catch (Exception e) {
            return new ToolResult("browser", false, "페이지 로드 오류");
        }
    }

    private boolean isPrivateHost(String host) {
        try {
            if ("localhost".equalsIgnoreCase(host)) return true;
            InetAddress addr = InetAddress.getByName(host);
            return addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isAnyLocalAddress();
        } catch (Exception e) {
            return true; // Fail-safe: block unknown hosts
        }
    }
}
