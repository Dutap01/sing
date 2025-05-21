package org.github.Dutap01.bot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BotTokenManager  {
    private String DiscordBotToken;

    public BotTokenManager() {
        settingToken();
    }

    private void settingToken() {
        try(InputStream input = getClass().getClassLoader().getResourceAsStream("DiscordBotToken.propertises")) {
            Properties properties = new Properties();
            properties.load(input);
            DiscordBotToken = properties.getProperty("Token");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDiscordBotToken() {
        return DiscordBotToken;
    }
}