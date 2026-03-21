package io.canvasmc.discord.util;

import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;

@NoArgsConstructor
public final class Embeds {

    private static final String CANVAS_LOGO = "https://avatars.githubusercontent.com/u/147121996?s=200&v=4";
    private static final String CANVAS_COLOR = "#2596be";

    public static EmbedBuilder canvas(String title) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(title);
        builder.setThumbnail(CANVAS_LOGO);
        builder.setColor(Color.decode(CANVAS_COLOR));
        return builder;
    }
}
