package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import io.canvasmc.discord.util.Embeds;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@DiscordCommand(name = "website", description = "CanvasMC's website")
public class WebsiteCommand implements CommandModule {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.replyEmbeds(
                        Embeds.canvas("Canvas Website")
                                .addField("Website", "https://canvasmc.io", false)
                                .build()
                )
                .queue();
    }
}
