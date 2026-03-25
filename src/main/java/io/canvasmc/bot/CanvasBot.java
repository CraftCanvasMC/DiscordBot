package io.canvasmc.bot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class CanvasBot {
    private static final Logger log = LoggerFactory.getLogger(CanvasBot.class);

    public static void main(String[] args) {
        String token = loadToken();
        if (token == null || token.isBlank() || token.equals("your-bot-token-here")) {
            log.error("BOT_TOKEN is not set! Create a .env file with BOT_TOKEN=<your-token>");
            System.exit(1);
        }

        GatewayDiscordClient client = DiscordClientBuilder.create(token)
                .build()
                .login()
                .block();

        if (client == null) {
            log.error("Failed to login to Discord!");
            System.exit(1);
        }

        client.updatePresence(ClientPresence.online(ClientActivity.watching("CanvasMC"))).block();

        registerCommands(client);

        SlashCommandListener listener = new SlashCommandListener();
        client.on(ChatInputInteractionEvent.class, listener::handle)
                .then()
                .subscribe();
        client.on(ButtonInteractionEvent.class, listener::handleButton)
            .then()
            .subscribe();

        log.info("CanvasMC Bot is now online!");
        client.onDisconnect().block();
    }

    private static void registerCommands(GatewayDiscordClient client) {
        try {
            List<String> files = List.of(
                    "about.json", "website.json", "project.json",
                    "docs.json", "git.json", "faq.json",
                    "optimizationguide.json", "download.json"
            );
            new GlobalCommandRegistrar(client.getRestClient()).registerCommands(files);
            log.info("Registered all global slash commands");
        } catch (IOException e) {
            log.error("Failed to register global commands!", e);
        }
    }

    private static String loadToken() {
        Path envFile = Path.of(".env");
        if (Files.exists(envFile)) {
            try {
                Properties props = new Properties();
                for (String line : Files.readAllLines(envFile)) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        props.setProperty(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                    }
                }
                String token = props.getProperty("BOT_TOKEN");
                if (token != null && !token.isBlank()) {
                    log.info("Loaded BOT_TOKEN from .env file");
                    return token;
                }
            } catch (IOException e) {
                log.warn("Failed to read .env file", e);
            }
        }

        String env = System.getenv("BOT_TOKEN");
        if (env != null && !env.isBlank()) {
            log.info("Loaded BOT_TOKEN from environment variable");
            return env;
        }

        return null;
    }
}
