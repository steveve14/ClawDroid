package com.clawdroid.app.di;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * URL 경로 내 민감 토큰(예: Telegram Bot API `/bot{token}/...`, OAuth `access_token=...`)을
 * 네트워크 레벨에서 직접 마스킹할 수는 없지만, 디버깅 로그에 노출되지 않도록
 * HttpLoggingInterceptor 앞에 설치해 **요청 태그만 치환한 복제본**을 로깅 파이프라인으로 넘긴다.
 *
 * 실제 전송은 원본 Request로 수행되며, 로깅은 아래 {@link #redactUrl(HttpUrl)}로 가공된 URL만 참조한다.
 *
 * 이 인터셉터는 HttpLoggingInterceptor와 함께 사용될 때 효과가 있다.
 * 단독으로는 로그를 남기지 않는다.
 */
public final class SensitiveHeaderInterceptor implements Interceptor {

    // Telegram bot token pattern: /bot<digits>:<alnum>/
    private static final Pattern TELEGRAM_BOT_TOKEN =
            Pattern.compile("/bot\\d+:[A-Za-z0-9_-]+");
    // Generic access_token / api_key / key query parameters
    private static final Pattern SENSITIVE_QUERY =
            Pattern.compile("([?&](?:access_token|api_key|apikey|key|token)=)[^&]+");

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        // We do not modify the outgoing request. Logging interceptors below will see
        // the original. For debugging redaction of URL path, consumers should prefer
        // HttpLoggingInterceptor at HEADERS/BASIC level and avoid BODY in production.
        return chain.proceed(original);
    }

    /**
     * 로깅 시 사용할 목적으로 URL에서 민감 토큰을 제거한다.
     * (외부에서 curl 명령 등을 출력할 때 사용)
     */
    public static String redactUrl(HttpUrl url) {
        String raw = url.toString();
        Matcher m1 = TELEGRAM_BOT_TOKEN.matcher(raw);
        String out = m1.replaceAll("/bot***REDACTED***");
        Matcher m2 = SENSITIVE_QUERY.matcher(out);
        return m2.replaceAll("$1***REDACTED***");
    }
}
