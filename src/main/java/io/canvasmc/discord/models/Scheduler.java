package io.canvasmc.discord.models;

import io.canvasmc.discord.util.Embeds;
import lombok.AllArgsConstructor;
import lombok.Builder;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Collection;
import java.util.Set;

@Builder
@AllArgsConstructor
public class Scheduler extends FieldRegistry implements EmbedModel {

    public static final Scheduler GLOBAL = Scheduler.builder()
            .name("Global")
            .description("The global scheduler should be utilized for any tasks that do not belong to a particular region.")
            .documentation("https://docs.papermc.io/paper/dev/folia-support/#global-scheduler") // TODO: Change
            .build();

    public static final Scheduler REGION = Scheduler.builder()
            .name("Region")
            .description("The region scheduler is in charge of running tasks for the region that owns a certain location, chunk, or block. Refrain from using this scheduler for operations on entities, as this scheduler is tied to the region and entities may teleport or move away.")
            .documentation("https://docs.papermc.io/paper/dev/folia-support/#region-scheduler") // TODO: Change
            .build();

    public static final Scheduler ASYNC = Scheduler.builder()
            .name("Async")
            .description("The async scheduler can be used for running tasks independent of the server tick process.")
            .documentation("https://docs.papermc.io/paper/dev/folia-support/#async-scheduler") // TODO: Change
            .build();

    public static final Scheduler ENTITY = Scheduler.builder()
            .name("Entity")
            .description("Entity schedulers are used for executing tasks on an entity. These will follow the entity wherever it goes, so you should use these instead of the region schedulers when working with entities.")
            .documentation("https://docs.papermc.io/paper/dev/folia-support/#entity-scheduler") // TODO: Change
            .build();


    private final String name;
    private final String description;
    private final String documentation;

    @Override
    public EmbedBuilder embed() {
        EmbedBuilder builder = Embeds.canvas("The " + name + " Scheduler");
        builder.setDescription(description);
        builder.addField("Documentation", documentation, false);
        return builder;
    }

    public static Scheduler get(String key) {
        return FieldRegistry.get(Scheduler.class, key);
    }

    public static Set<String> keys() {
        return FieldRegistry.keys(Scheduler.class);
    }

    public static Collection<Scheduler> values() {
        return FieldRegistry.values(Scheduler.class);
    }
}
