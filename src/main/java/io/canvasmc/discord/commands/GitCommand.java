package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import io.canvasmc.discord.util.Embeds;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@DiscordCommand(name = "git", description = "CanvasMC GitHub organization")
public class GitCommand implements CommandModule {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.replyEmbeds(
                Embeds.canvas("Canvas GitHub")
                        .addField("GitHub", "https://github.com/CraftCanvasMC", false)
                        .build()
                )
                .queue();
    }
}
