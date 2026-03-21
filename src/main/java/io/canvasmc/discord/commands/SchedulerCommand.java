package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import io.canvasmc.discord.models.Scheduler;
import io.canvasmc.discord.util.OptionUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

@DiscordCommand(name = "scheduler", description = "Learn about the various schedulers available")
public class SchedulerCommand implements CommandModule {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String string = OptionUtil.getOption(event.getOption("scheduler"), OptionType.STRING);

        Scheduler scheduler = Scheduler.get(string);
        if (scheduler == null) {
            event.reply("Scheduler not found").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(scheduler.embed().build()).queue();
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "scheduler", "The scheduler type", true)
                        .addChoices(OptionUtil.buildChoicesFromKeys(Scheduler.keys()))
        );
    }
}
