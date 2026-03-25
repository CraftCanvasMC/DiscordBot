package io.canvasmc.bot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateFields;
import io.canvasmc.bot.model.Faq;
import io.canvasmc.bot.model.Project;
import io.canvasmc.bot.util.DownloadService;
import io.canvasmc.bot.util.DocsSearchService;
import io.canvasmc.bot.util.Embeds;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;

public class SlashCommandListener {
    private final DocsSearchService docsSearchService = DocsSearchService.getInstance();
    private final DownloadService downloadService = DownloadService.getInstance();

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return switch (event.getCommandName()) {
            case "about" -> handleAbout(event);
            case "website" -> handleWebsite(event);
            case "project" -> handleProject(event);
            case "docs" -> handleDocs(event);
            case "git" -> handleGit(event);
            case "faq" -> handleFaq(event);
            case "download" -> handleDownload(event);
            case "optimizationguide" -> handleOptimizationGuide(event);
            default -> Mono.empty();
        };
    }

    public Mono<Void> handleButton(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith("download:latest:")) {
            return Mono.empty();
        }

        String project = customId.substring("download:latest:".length());
        if (!isSupportedProject(project)) {
            return event.reply("Unsupported project on this button.").withEphemeral(true);
        }

        return Mono.fromCallable(() -> downloadService.getLatestBuild(project))
                .flatMap(latest -> {
                    EmbedCreateSpec embed = Embeds.canvas("Latest " + toDisplayName(project) + " Build")
                            .description("Use the button below to download the newest build.")
                            .addField("Build", "#" + latest.buildNumber(), true)
                            .addField("Channel", latest.channelVersion() == null ? "Unknown" : latest.channelVersion(), true)
                            .build();

                    InteractionApplicationCommandCallbackSpec reply = InteractionApplicationCommandCallbackSpec.builder()
                            .ephemeral(true)
                            .addEmbed(embed)
                            .addComponent(ActionRow.of(Button.link(latest.downloadUrl(), "Download latest build")))
                            .addFile(Embeds.logoAttachment())
                            .build();
                    return event.reply(reply);
                })
                .onErrorResume(error -> event.reply("I couldn't fetch the latest build right now.").withEphemeral(true));
    }

    private Mono<Void> reply(ChatInputInteractionEvent event, EmbedCreateSpec embed) {
        return event.reply()
                .withEmbeds(embed)
                .withFiles(Embeds.logoAttachment());
    }

    private String getOption(ChatInputInteractionEvent event, String name) {
        return event.getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");
    }

    private Mono<Void> handleAbout(ChatInputInteractionEvent event) {
        return reply(event, Embeds.canvas("About CanvasMC")
                .description("CanvasMC is a fork of the Folia Minecraft server software that fixes gameplay inconsistencies, bugs, and introduces further performance enhancements to the dedicated server")
                .build());
    }

    private Mono<Void> handleWebsite(ChatInputInteractionEvent event) {
        return reply(event, Embeds.canvas("CanvasMC Website")
                .description("You can find our website at https://canvasmc.io/")
                .build());
    }

    private Mono<Void> handleProject(ChatInputInteractionEvent event) {
        Project project = Project.fromName(getOption(event, "project"));
        if (project == null) {
            return event.reply("Unknown project!").withEphemeral(true);
        }
        return reply(event, project.toEmbed());
    }

    private Mono<Void> handleDocs(ChatInputInteractionEvent event) {
        String project = getOption(event, "project");
        String keyword = getOption(event, "keyword");

        if (keyword == null || keyword.isBlank()) {
            return reply(event, Embeds.canvas("CanvasMC Documentation")
                    .description("You can find our documentation at https://docs.canvasmc.io/")
                    .build());
        }

        DocsSearchService.SearchResult result = docsSearchService.search(project, keyword, 6);
        if (result.state() == DocsSearchService.State.ERROR) {
            return event.reply(result.message()).withEphemeral(true);
        }
        if (result.state() == DocsSearchService.State.INDEXING) {
            return event.reply(result.message()).withEphemeral(true);
        }

        List<DocsSearchService.DocHit> hits = result.hits();
        if (hits.isEmpty()) {
            String projectLabel = project == null || project.isBlank() ? "all" : project;
            return reply(event, Embeds.canvas("CanvasMC Docs Search")
                    .description("No results found for **" + keyword + "** in **" + projectLabel + "** docs.")
                    .addField("Browse Docs", "https://docs.canvasmc.io/", false)
                    .build());
        }

        StringBuilder links = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            DocsSearchService.DocHit hit = hits.get(i);
            links.append(i + 1)
                    .append(". [")
                    .append(hit.title())
                    .append("](")
                    .append(hit.url())
                    .append(")\n");
        }

        return reply(event, Embeds.canvas("CanvasMC Docs Search")
                .description("Top matches for **" + keyword + "**")
                .addField("Results", links.toString().trim(), false)
                .addField("Indexed Pages", String.valueOf(result.indexedPages()), true)
                .build());
    }

    private Mono<Void> handleGit(ChatInputInteractionEvent event) {
        return reply(event, Embeds.canvas("CanvasMC GitHub")
                .description("You can find our GitHub organization at https://github.com/CraftCanvasMC")
                .build());
    }

    private Mono<Void> handleFaq(ChatInputInteractionEvent event) {
        Faq faq = Faq.fromName(getOption(event, "type"));
        if (faq == null) {
            return event.reply("Unknown FAQ type!").withEphemeral(true);
        }

        String mention = event.getOption("for_user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(val -> "<@" + val.asSnowflake().asString() + "> ")
                .orElse("");

        if (faq == Faq.FOLIASPREAD) {
            InputStream img = getClass().getClassLoader().getResourceAsStream("foliaspread.png");
            if (img == null) {
                return event.reply("Could not load the spread image.").withEphemeral(true);
            }

            EmbedCreateSpec embed = Embeds.canvas(faq.title())
                    .description(faq.description())
                    .image("attachment://foliaspread.png")
                    .build();

            InteractionApplicationCommandCallbackSpec.Builder reply = InteractionApplicationCommandCallbackSpec.builder()
                    .addEmbed(embed)
                    .addFile(Embeds.logoAttachment())
                    .addFile(MessageCreateFields.File.of("foliaspread.png", img));

            if (!mention.isEmpty()) {
                reply.content(mention);
            }

            return event.reply(reply.build());
        }

        InteractionApplicationCommandCallbackSpec.Builder reply = InteractionApplicationCommandCallbackSpec.builder()
                .addEmbed(faq.toEmbed())
                .addFile(Embeds.logoAttachment());

        if (!mention.isEmpty()) {
            reply.content(mention);
        }

        return event.reply(reply.build());
    }

    private Mono<Void> handleOptimizationGuide(ChatInputInteractionEvent event) {
        return reply(event, Embeds.canvas("Server Optimization Guide")
                .description("Check out our comprehensive server optimization guide to get the best performance out of your CanvasMC server.")
                .addField("Link", "[Optimization Guide on Docs](https://docs.canvasmc.io)", false)
                .build());
    }

    private Mono<Void> handleDownload(ChatInputInteractionEvent event) {
        String project = getOption(event, "project");
        String channel = getOption(event, "channel");

        if ((project == null || project.isBlank()) && (channel == null || channel.isBlank())) {
            InteractionApplicationCommandCallbackSpec reply = InteractionApplicationCommandCallbackSpec.builder()
                    .addEmbed(Embeds.canvas("CanvasMC Downloads")
                            .description("Open the downloads page to choose a project and build.")
                            .build())
                    .addComponent(ActionRow.of(Button.link(DownloadService.downloadsPage(null), "Open downloads page")))
                    .addFile(Embeds.logoAttachment())
                    .build();
            return event.reply(reply);
        }

        if ((project == null || project.isBlank()) && channel != null && !channel.isBlank()) {
            return event.reply("Please provide a project when using the channel filter.").withEphemeral(true);
        }

        if (!isSupportedProject(project)) {
            return event.reply("Unknown project! Supported values: canvas, horizon.").withEphemeral(true);
        }

        String normalizedProject = project.trim().toLowerCase();
        String trimmedChannel = channel == null ? "" : channel.trim();

        return Mono.fromCallable(() -> downloadService.getBuilds(normalizedProject, trimmedChannel))
                .flatMap(builds -> {
                    List<DownloadService.BuildInfo> newest = builds.stream().limit(3).toList();

                    if (!trimmedChannel.isBlank() && newest.isEmpty()) {
                        return event.reply("No builds were found for channel '" + trimmedChannel + "'.").withEphemeral(true);
                    }

                    String title = trimmedChannel.isBlank()
                            ? "Newest " + toDisplayName(normalizedProject) + " Builds"
                            : "Newest " + toDisplayName(normalizedProject) + " Builds for " + trimmedChannel;

                    EmbedCreateSpec.Builder embedBuilder = Embeds.canvas(title)
                            .description(newest.isEmpty()
                                    ? "No downloadable builds were found for this filter."
                                    : "Showing the 3 newest downloadable builds.");

                    for (DownloadService.BuildInfo build : newest) {
                        embedBuilder.addField(
                                "Build #" + build.buildNumber(),
                                "Channel: " + (build.channelVersion() == null ? "Unknown" : build.channelVersion()) +
                                        "\n[Download JAR](" + build.downloadUrl() + ")",
                                false
                        );
                    }

                    InteractionApplicationCommandCallbackSpec.Builder response = InteractionApplicationCommandCallbackSpec.builder()
                            .addEmbed(embedBuilder.build())
                            .addFile(Embeds.logoAttachment());

                    if (!newest.isEmpty()) {
                        List<Button> buildButtons = newest.stream()
                                .map(b -> Button.link(b.downloadUrl(), "Build #" + b.buildNumber()))
                                .toList();
                        response.addComponent(ActionRow.of(buildButtons));
                    }

                    return event.reply(response.build());
                })
                .onErrorResume(error -> event.reply("I couldn't fetch builds from the downloads API right now.").withEphemeral(true));
    }

    private boolean isSupportedProject(String project) {
        if (project == null || project.isBlank()) {
            return false;
        }
        String normalized = project.trim().toLowerCase();
        return normalized.equals("canvas") || normalized.equals("horizon");
    }

    private String toDisplayName(String project) {
        return project.equalsIgnoreCase("horizon") ? "Horizon" : "Canvas";
    }
}
