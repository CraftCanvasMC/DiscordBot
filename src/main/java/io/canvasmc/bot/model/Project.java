package io.canvasmc.bot.model;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import io.canvasmc.bot.util.Embeds;

public enum Project {
    CANVAS("Canvas", "The high-performance Minecraft server fork.", "https://github.com/CraftCanvasMC/Canvas"),
    HORIZON("Horizon", "The multithreaded API for Canvas.", "https://github.com/CraftCanvasMC/Horizon");

    private final String name;
    private final String desc;
    private final String github;

    Project(String name, String desc, String github) {
        this.name = name;
        this.desc = desc;
        this.github = github;
    }

    public String displayName() { return name; }
    public String description() { return desc; }
    public String githubUrl() { return github; }

    public EmbedCreateSpec toEmbed() {
        return Embeds.canvas(name)
                .description(desc)
                .addField("GitHub", "[Source](" + github + ")", true)
                .addFields(EmbedCreateFields.Field.of("Downloads", "[Website](https://canvasmc.io/downloads/" + name.toLowerCase() + ")", true))
                .build();
    }

    public static Project fromName(String input) {
        try {
            return valueOf(input.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
