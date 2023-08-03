package dev.emortal.api.command;

import dev.emortal.api.command.element.CommandElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public record CommandSyntax<S>(@Nullable Predicate<S> condition, @NotNull CommandExecutor<S> executor, @NotNull List<CommandElement<S>> elements) {
}
