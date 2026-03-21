package io.canvasmc.discord.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.util.Collection;
import java.util.Set;

@Builder
@AllArgsConstructor
public class Project extends FieldRegistry implements EmbedModel {

    public static final Project CANVAS = Project.builder()
            .name("Canvas")
            .description("CanvasMC is a fork of the Folia Minecraft server software that fixes gameplay inconsistencies, bugs, and introduces further performance enhancements to the dedicated server.")
            .website("https://canvasmc.io")
            .documentation("https://docs.canvasmc.io/canvas/introduction")
            .github("https://github.com/CraftCanvasMC/Canvas")
            .logo("https://avatars.githubusercontent.com/u/147121996?s=200&v=4")
            .color("#2596be")
            .build();

    public static final Project HORIZON = Project.builder()
            .name("Horizon")
            .description("Horizon is a mixin wrapper for PaperMC servers and forks, expanding plugin capabilities to allow for further customization and enhancements.")
            .website("https://docs.canvasmc.io/horizon/introduction")
            .documentation("https://docs.canvasmc.io/horizon/introduction")
            .github("https://github.com/CraftCanvasMC/Horizon")
            .logo("https://github.com/CraftCanvasMC/Horizon/raw/main/assets/horizon_logo.png")
            .color("#ffc606")
            .build();


    private final String name;
    private final String description;
    private final String website;
    private final String documentation;
    private final String github;
    private final String logo;
    private final String color;

    @Override
    public EmbedBuilder embed() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(name);
        builder.setDescription(description);
        builder.addField("Website", website, true);
        builder.addField("Documentation", documentation, true);
        builder.addField("GitHub", github, true);
        builder.setColor(Color.decode(color));
        return builder;
    }

    public static Project get(String key) {
        return FieldRegistry.get(Project.class, key);
    }

    public static Set<String> keys() {
        return FieldRegistry.keys(Project.class);
    }

    public static Collection<Project> values() {
        return FieldRegistry.values(Project.class);
    }
}