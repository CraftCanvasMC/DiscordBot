package io.canvasmc.bot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import io.canvasmc.bot.util.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.io.IOException;

public class CanvasBot {
    private static final Logger log = LoggerFactory.getLogger(CanvasBot.class);

    public static void main(String[] args) {
        EnvConfig env = EnvConfig.load();
        String token = loadToken(env);
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

        BuildAnnouncementService.create(client, env).start();

        log.info("CanvasMC Bot is now online!");
        client.onDisconnect().block();
    }

    private static void registerCommands(GatewayDiscordClient client) {
        try {
            List<String> files = List.of(
                    "about.json", "website.json", "project.json",
                    "docs.json", "git.json", "faq.json",
                    //"optimizationguide.json",
                    "download.json"
            );
            new GlobalCommandRegistrar(client.getRestClient()).registerCommands(files);
            log.info("Registered all global slash commands");
        } catch (IOException e) {
            log.error("Failed to register global commands!", e);
        }
    }

    private static String loadToken(EnvConfig env) {
        String token = env.get("BOT_TOKEN");
        if (token != null && !token.isBlank()) {
            log.info("Loaded BOT_TOKEN from configuration");
            return token;
        }
        return null;
    }
}
