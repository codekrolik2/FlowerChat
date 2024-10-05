package com.vivo.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatClientLauncher {
    final static Logger LOGGER = LoggerFactory.getLogger(ChatClientLauncher.class);

    public static void main(String[] args) {
        LOGGER.info("Starting Chat Client");
        ChatClientApplication.main(args);
    }
}
