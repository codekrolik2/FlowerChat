package com.flower.chat.okhttp;

import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.annotation.Nullable;

public class HttpBase {
    public static final HttpLoggingInterceptor LOGGING_INTERCEPTOR =
            new HttpLoggingInterceptor(message -> System.out.println(message));
    private static final OkHttpClient HTTP_CLIENT = buildHttpClient();

    public static OkHttpClient buildHttpClient() {
        return buildHttpClient(null);
    }

    public static OkHttpClient buildHttpClient(@Nullable Interceptor interceptor) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(100);
        dispatcher.setMaxRequests(100);
        OkHttpClient.Builder builder = (new OkHttpClient.Builder()).dispatcher(dispatcher);
        if (interceptor != null) {
            builder.addInterceptor(interceptor);
        }
        builder.addInterceptor(LOGGING_INTERCEPTOR);

        return builder.build();
    }

    public static WebSocket newWebSocket(Request request, WebSocketListener webSocketListener) {
        return HTTP_CLIENT.newWebSocket(request, webSocketListener);
    }

    public static void shutdownHttp() {
        HTTP_CLIENT.dispatcher().executorService().shutdown();
        HTTP_CLIENT.connectionPool().evictAll();
        HTTP_CLIENT.dispatcher().cancelAll();
    }
}
