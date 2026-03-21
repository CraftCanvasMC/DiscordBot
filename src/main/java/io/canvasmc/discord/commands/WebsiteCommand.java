package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@DiscordCommand(name = "website", description = "CanvasMC's website")
public class WebsiteCommand implements CommandModule {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("https://canvasmc.io").queue();
    }
}
