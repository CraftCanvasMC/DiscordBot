package io.canvasmc.bot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateFields;
import io.canvasmc.bot.model.Faq;
import io.canvasmc.bot.model.Project;
import io.canvasmc.bot.util.Embeds;
import reactor.core.publisher.Mono;

import java.io.InputStream;

public class SlashCommandListener {

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return switch (event.getCommandName()) {
            case "about" -> handleAbout(event);
            case "website" -> handleWebsite(event);
            case "project" -> handleProject(event);
            case "docs" -> handleDocs(event);
            case "git" -> handleGit(event);
            case "faq" -> handleFaq(event);
            case "optimizationguide" -> handleOptimizationGuide(event);
            default -> Mono.empty();
        };
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
        return reply(event, Embeds.canvas("CanvasMC Documentation")
                .description("You can find our documentation at https://docs.canvasmc.io/")
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
}
