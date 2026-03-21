package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import io.canvasmc.discord.util.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@DiscordCommand(name = "foliaspread", description = "Folia spread image")
public class FoliaSpreadCommand implements CommandModule {

    // Maybe get this image from a better source
    private static final String IMAGE_URL = "https://iili.io/qeSAgvR.png";

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = Embeds.canvas("Folia Spread");
        embed.setImage(IMAGE_URL);
        embed.setDescription("Canvas will run best when players are spread out. Make sure your server's game mode encourages this behavior!");
        event.replyEmbeds(embed.build()).queue();
    }
}
