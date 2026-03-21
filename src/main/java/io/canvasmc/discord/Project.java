package io.canvasmc.discord;

import lombok.Builder;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Builder
public record Project(String name,
                      String description,
                      String website,
                      String documentation,
                      String github,
                      String logo,
                      String color) {

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

    public EmbedBuilder embed() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(name);
        builder.setDescription(description);
        builder.setThumbnail(logo);
        builder.addField("Website", website, true);
        builder.addField("Documentation", documentation, true);
        builder.addField("GitHub", github, true);
        builder.setColor(Color.decode(color));
        return builder;
    }


    private static final Map<String, Project> KEYS = new LinkedHashMap<>();

    public static Project get(String key) {
        return KEYS.get(key);
    }

    public static Set<String> keys() {
        return KEYS.keySet();
    }

    public static Collection<Project> values() {
        return KEYS.values();
    }


    static {
        for (Field field : Project.class.getDeclaredFields()) {
            if (Project.class.isAssignableFrom(field.getType())) {
                try {
                    Project project = (Project) field.get(null);
                    KEYS.put(field.getName(), project);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
