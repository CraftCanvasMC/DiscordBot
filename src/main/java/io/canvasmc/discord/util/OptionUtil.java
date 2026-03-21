package io.canvasmc.discord.util;

import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.Nullable;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
public final class OptionUtil {

    @Nullable
    public static <T> T getOption(OptionMapping optionMapping, OptionType optionType) {
        return getOption(optionMapping, optionType, null);
    }

    public static <T> T getOption(OptionMapping optionMapping, OptionType optionType, @Nullable T defaultValue) {
        if (optionMapping == null) return defaultValue;

        return switch (optionType) {
            case UNKNOWN, SUB_COMMAND, SUB_COMMAND_GROUP -> defaultValue;
            case STRING -> (T) optionMapping.getAsString();
            case INTEGER -> (T) Integer.valueOf(optionMapping.getAsInt());
            case BOOLEAN -> (T) Boolean.valueOf(optionMapping.getAsBoolean());
            case CHANNEL -> (T) optionMapping.getAsChannel();
            case ROLE -> (T) optionMapping.getAsRole();
            case MENTIONABLE -> (T) optionMapping.getAsMentionable();
            case ATTACHMENT -> (T) optionMapping.getAsAttachment();
            case USER -> {
                try {
                    yield (T) optionMapping.getAsMember();
                } catch (IllegalStateException e) {
                    yield (T) optionMapping.getAsUser();
                }
            }
            case NUMBER -> {
                try {
                    yield (T) Double.valueOf(optionMapping.getAsDouble());
                } catch (IllegalStateException e) {
                    yield (T) Long.valueOf(optionMapping.getAsLong());
                }
            }
        };
    }

    public static <E extends Enum<E>> List<Command.Choice> buildChoicesFromEnum(Class<E> enumClass, String... ignore) {
        Set<String> ignoreSet = Set.of(ignore);
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> !ignoreSet.contains(e.name()))
                .map(e -> new Command.Choice(e.name().toLowerCase(), e.name()))
                .collect(Collectors.toList());
    }


    public static List<Command.Choice> buildChoicesFromKeys(Collection<String> keys, String... ignore) {
        Set<String> ignoreSet = Set.of(ignore);
        return keys.stream()
                .filter(key -> !ignoreSet.contains(key))
                .map(key -> new Command.Choice(key.toLowerCase(), key))
                .collect(Collectors.toList());
    }
}
