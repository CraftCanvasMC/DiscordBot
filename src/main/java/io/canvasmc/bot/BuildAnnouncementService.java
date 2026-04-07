package io.canvasmc.bot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import io.canvasmc.bot.util.DownloadService;
import io.canvasmc.bot.util.Embeds;
import io.canvasmc.bot.util.EnvConfig;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BuildAnnouncementService {
    private static final Logger log = LoggerFactory.getLogger(BuildAnnouncementService.class);

    private static final Duration START_DELAY = Duration.ofSeconds(20);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(45);

    private final GatewayDiscordClient client;
    private final DownloadService downloadService;
    private final List<ProjectChannels> projects;
    private final Map<String, Integer> lastSeenBuild = new ConcurrentHashMap<>();

    private Disposable pollSubscription;

    private BuildAnnouncementService(GatewayDiscordClient client, DownloadService downloadService, List<ProjectChannels> projects) {
        this.client = client;
        this.downloadService = downloadService;
        this.projects = projects;
    }

    public static BuildAnnouncementService create(GatewayDiscordClient client, EnvConfig env) {
        List<ProjectChannels> configured = new ArrayList<>();

        ProjectChannels canvas = buildProjectConfig("canvas", env.get("CanvasRoleID", "CANVAS_ROLE_ID"), env.get("CanvasHelpID", "CANVAS_HELP_ID"), env.get("CanvasDevID", "CANVAS_DEV_ID"));
        if (canvas != null) {
            configured.add(canvas);
        }

        ProjectChannels horizon = buildProjectConfig("horizon", env.get("HorizonRoleID", "HORIZON_ROLE_ID"), env.get("HorizonHelpID", "HORIZON_HELP_ID"), env.get("HorizonDevID", "HORIZON_DEV_ID"));
        if (horizon != null) {
            configured.add(horizon);
        }

        return new BuildAnnouncementService(client, DownloadService.getInstance(), configured);
    }

    public void start() {
        if (projects.isEmpty()) {
            log.info("No project role/channel IDs configured");
            return;
        }

        this.pollSubscription = Flux.interval(START_DELAY, POLL_INTERVAL)
                .flatMap(tick -> pollOnce())
                .onErrorContinue((throwable, ignored) -> log.warn("Build poll loop failed", throwable))
                .subscribe();

        log.info("Polling started for {} projects.", projects.size());
    }

    private Mono<Void> pollOnce() {
        return Flux.fromIterable(projects)
                .flatMap(this::checkProject)
                .then();
    }

    private Mono<Void> checkProject(ProjectChannels project) {
        return Mono.fromCallable(() -> downloadService.getLatestBuild(project.projectKey))
                .flatMap(latest -> {
                    Integer previous = lastSeenBuild.putIfAbsent(project.projectKey, latest.buildNumber());
                    if (previous == null) {
                        log.info("Initialized {} build tracker at #{}", project.projectKey, latest.buildNumber());
                        return Mono.empty();
                    }

                    if (latest.buildNumber() <= previous) {
                        return Mono.empty();
                    }

                    lastSeenBuild.put(project.projectKey, latest.buildNumber());
                    return announce(project, latest);
                })
                .onErrorResume(error -> {
                    log.warn("Failed to poll latest {} build", project.projectKey, error);
                    return Mono.empty();
                });
    }

    private Mono<Void> announce(ProjectChannels project, DownloadService.BuildInfo build) {
        String displayName = project.projectKey.equals("horizon") ? "Horizon" : "Canvas";
        String mention = "<@&" + project.roleId + ">";
        String trackedDownloadUrl = build.trackedDownloadUrl();

        StringBuilder builder = new StringBuilder("A new build is now available for download.\n");
        if (!build.commits().isEmpty()) {
            builder.append("```");
            List<DownloadService.BuildInfo.Commit> commits = build.commits();
            for (int i = 0; i < commits.size(); i++) {
                final DownloadService.BuildInfo.Commit commit = commits.get(i);
                String extra = commit.extraDescription;
                String wrappedExtra = wordWrap(extra, 35, "   | ");
                builder.append(" - ").append(commit.message) // commit msg first
                    .append(" [").append(commit.author).append("]\n"); // then the author
                // add commit description if present
                if (commit.extraDescription != null && !commit.extraDescription.isEmpty()) {
                    builder.append("   | ").append(wrappedExtra).append(i == (commits.size() - 1) ? "" : "\n");
                }
            }
            builder.append("```");
        } else builder.append("There is no changelog associated with this build");
        EmbedCreateSpec embed = Embeds.canvas("New " + displayName + " Build Released")
                .description(builder.toString())
                .addField("Build", "#" + build.buildNumber(), true)
                .addField("Channel", build.channelVersion() == null || build.channelVersion().isBlank() ? "Unknown" : build.channelVersion(), true)
                .addField("Download", "[Get build](" + trackedDownloadUrl + ")", false)
                .build();

        return Mono.whenDelayError(sendAnnouncement(project.helpChannelId, "", embed, trackedDownloadUrl), sendAnnouncement(project.devChannelId, mention, embed, trackedDownloadUrl))
                .doOnSuccess(ignored -> log.info("Announced {} build #{}", project.projectKey, build.buildNumber()))
                .then();
    }

    private @NonNull String wordWrap(String text, int maxWidth, String indent) {
        if (text == null || text.length() <= maxWidth) {
            return text == null ? "" : text;
        }

        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+");
        int lineLength = 0;

        for (String word : words) {
            if (lineLength == 0) {
                result.append(word);
                lineLength = word.length();
            }
            else if (lineLength + 1 + word.length() > maxWidth) {
                result.append("\n").append(indent).append(word);
                lineLength = word.length();
            }
            else {
                result.append(" ").append(word);
                lineLength += 1 + word.length();
            }
        }

        return result.toString();
    }

    private Mono<Void> sendAnnouncement(String channelId, String mention, EmbedCreateSpec embed, String downloadUrl) {
        MessageCreateSpec message = MessageCreateSpec.builder()
                .content(mention)
                .addEmbed(embed)
                .addFile(Embeds.logoAttachment())
                .addComponent(ActionRow.of(Button.link(downloadUrl, "Download build")))
                .build();

        return Mono.defer(() -> client.getChannelById(Snowflake.of(channelId))
                        .ofType(MessageChannel.class)
                        .flatMap(channel -> channel.createMessage(message))
                        .then())
                .onErrorResume(error -> {
                    log.warn("Failed to send announcement to channel {}", channelId, error);
                    return Mono.empty();
                });
    }

    private static ProjectChannels buildProjectConfig(String projectKey, String roleId, String helpChannelId, String devChannelId) {
        if (isBlank(roleId) || isBlank(helpChannelId) || isBlank(devChannelId)) {
            return null;
        }
        return new ProjectChannels(projectKey, roleId.trim(), helpChannelId.trim(), devChannelId.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ProjectChannels(String projectKey, String roleId, String helpChannelId, String devChannelId) {}
}