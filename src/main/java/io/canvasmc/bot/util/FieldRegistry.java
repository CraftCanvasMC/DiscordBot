package io.canvasmc.bot.util;

import discord4j.core.spec.EmbedCreateFields;

public final class FieldRegistry {
    private FieldRegistry() {}
    public static final EmbedCreateFields.Field SOURCE = EmbedCreateFields.Field.of("Source", "[GitHub](https://github.com/CraftCanvasMC)", true);
    public static final EmbedCreateFields.Field DOWNLOADS = EmbedCreateFields.Field.of("Downloads", "[Website](https://canvasmc.io/downloads)", true);
    public static final EmbedCreateFields.Field DISCORD = EmbedCreateFields.Field.of("Discord", "[Join](https://discord.gg/canvasmc)", true);
}
