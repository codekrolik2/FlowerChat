/*
 * Copyright 2022 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.flower.chat.server;

import com.flower.chat.trust.ChatTrust;
import com.flower.utils.PkiUtil;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;

/**
 * Some useful methods for server side.
 */
public final class ServerUtil {
    public static final KeyManagerFactory SERVER_KEY_MANAGER =
            PkiUtil.getKeyManagerFromResources("chat_server.crt", "chat_server.key", "");

    private ServerUtil() {
    }

    public static SslContext buildSslContext() throws SSLException {
        return SslContextBuilder
                .forServer(SERVER_KEY_MANAGER)
                .trustManager(ChatTrust.TRUST_MANAGER)
                .clientAuth(ClientAuth.REQUIRE)
                .build();
    }
}
