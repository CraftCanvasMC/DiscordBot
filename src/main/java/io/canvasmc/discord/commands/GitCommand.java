package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@DiscordCommand(name = "git", description = "CanvasMC Github organization")
public class GitCommand implements CommandModule {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("https://github.com/CraftCanvasMC").queue();
    }
}
