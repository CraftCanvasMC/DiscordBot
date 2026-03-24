package io.canvasmc.bot.util;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.rest.util.Color;

import java.io.InputStream;
import java.time.Instant;

public final class Embeds {

    private static final Color BRAND_COLOR = Color.of(0x34C1FA);
    private static final String LOGO = "attachment://logo.png";

    private Embeds() {}

    public static EmbedCreateSpec.Builder canvas(String title) {
        return EmbedCreateSpec.builder()
                .color(BRAND_COLOR)
                .title(title)
                .thumbnail(LOGO)
                .footer("CanvasMC", LOGO)
                .timestamp(Instant.now());
    }

    public static MessageCreateFields.File logoAttachment() {
        InputStream stream = Embeds.class.getClassLoader().getResourceAsStream("logo.png");
        if (stream == null) throw new IllegalStateException("logo.png not found in resources!");
        return MessageCreateFields.File.of("logo.png", stream);
    }
}
