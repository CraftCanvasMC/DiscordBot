package io.canvasmc.discord.commands;

import dev.jsinco.discord.framework.commands.CommandModule;
import dev.jsinco.discord.framework.commands.DiscordCommand;
import io.canvasmc.discord.Project;
import io.canvasmc.discord.util.OptionUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

@DiscordCommand(name = "project", description = "Get information about a project")
public class ProjectCommand implements CommandModule {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String projectString = OptionUtil.getOption(event.getOption("project"), OptionType.STRING);

        Project project = Project.get(projectString);
        if (project == null) {
            event.reply("Project not found").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(project.embed().build()).queue();
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "project", "The project to get information about", true)
                        .addChoices(OptionUtil.buildChoicesFromKeys(Project.keys()))
        );
    }

}
