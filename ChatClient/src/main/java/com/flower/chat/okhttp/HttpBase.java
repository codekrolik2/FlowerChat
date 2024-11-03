package com.flower.chat.okhttp;

import com.flower.chat.trust.ChatTrust;
import com.flower.utils.PkiUtil;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.annotation.Nullable;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpBase {
    public static final HttpLoggingInterceptor LOGGING_INTERCEPTOR =
            new HttpLoggingInterceptor(message -> System.out.println(message));
    @Nullable private static OkHttpClient HTTP_CLIENT;

    public static void initHttpClient(String libraryPath, String pin) {
        HTTP_CLIENT = buildHttpClient(libraryPath, pin);
    }

    public static OkHttpClient buildHttpClient(String libraryPath, String pin) {
        return buildHttpClient(null, libraryPath, pin);
    }

    public static OkHttpClient buildHttpClient(@Nullable Interceptor interceptor, String libraryPath, String pin) {
        try {
            KeyManagerFactory keyManagerFactory = PkiUtil.getKeyManagerFromPKCS11(libraryPath, pin);
            TrustManagerFactory trustManagerFactory = ChatTrust.TRUST_MANAGER_WITH_SERVER_CA;

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequestsPerHost(100);
            dispatcher.setMaxRequests(100);
            OkHttpClient.Builder builder = (new OkHttpClient.Builder()).dispatcher(dispatcher)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagerFactory.getTrustManagers()[0]);
            //TODO: this shouldn't be needed with established server hostname and matching certificate
            builder.hostnameVerifier((s, sslSession) -> true);

            if (interceptor != null) {
                builder.addInterceptor(interceptor);
            }
            builder.addInterceptor(LOGGING_INTERCEPTOR);

            return builder.build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static WebSocket newWebSocket(Request request, WebSocketListener webSocketListener) {
        return checkNotNull(HTTP_CLIENT).newWebSocket(request, webSocketListener);
    }

    public static void shutdownHttp() {
        checkNotNull(HTTP_CLIENT).dispatcher().executorService().shutdown();
        checkNotNull(HTTP_CLIENT).connectionPool().evictAll();
        checkNotNull(HTTP_CLIENT).dispatcher().cancelAll();
    }
}
