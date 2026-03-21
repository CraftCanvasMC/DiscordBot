package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import io.canvasmc.discord.util.Embeds;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@DiscordCommand(name = "docs", description = "General documentation")
public class DocsCommand implements CommandModule {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.replyEmbeds(
                        Embeds.canvas("Documentation")
                                .addField("Docs", "https://docs.canvasmc.io", false)
                                .build()
                )
                .queue();
    }
}
