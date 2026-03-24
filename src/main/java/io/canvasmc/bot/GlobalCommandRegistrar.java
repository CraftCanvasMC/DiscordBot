package io.canvasmc.bot;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GlobalCommandRegistrar {
    private static final Logger log = LoggerFactory.getLogger(GlobalCommandRegistrar.class);
    private static final String COMMANDS_DIR = "commands/";

    private final RestClient restClient;

    public GlobalCommandRegistrar(RestClient restClient) {
        this.restClient = restClient;
    }

    public void registerCommands(List<String> fileNames) throws IOException {
        JacksonResources mapper = JacksonResources.create();
        ApplicationService appService = restClient.getApplicationService();
        long appId = restClient.getApplicationId().block();

        List<ApplicationCommandRequest> commands = new ArrayList<>();
        for (String json : readCommandFiles(fileNames)) {
            commands.add(mapper.getObjectMapper().readValue(json, ApplicationCommandRequest.class));
        }

        appService.bulkOverwriteGlobalApplicationCommand(appId, commands)
                .doOnNext(cmd -> log.debug("Registered global command: {}", cmd.name()))
                .doOnError(e -> log.error("Failed to register global commands", e))
                .subscribe();
    }

    private static List<String> readCommandFiles(List<String> fileNames) throws IOException {
        Objects.requireNonNull(
                GlobalCommandRegistrar.class.getClassLoader().getResource(COMMANDS_DIR),
                COMMANDS_DIR + " not found in resources"
        );

        List<String> result = new ArrayList<>();
        for (String file : fileNames) {
            String content = readResource(COMMANDS_DIR + file);
            result.add(Objects.requireNonNull(content, "Command file not found: " + file));
        }
        return result;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
