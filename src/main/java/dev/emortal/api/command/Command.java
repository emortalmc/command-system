package dev.emortal.api.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.emortal.api.command.element.CommandElement;
import dev.emortal.api.command.element.LiteralElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public abstract class Command<S> {

    private final String name;
    private final String[] aliases;

    private @Nullable Predicate<S> condition;
    private @Nullable CommandExecutor<S> defaultExecutor;

    private final List<CommandSyntax<S>> syntaxes = new ArrayList<>();
    private final List<Command<S>> subCommands = new ArrayList<>();

    public Command(@NotNull String name, @NotNull String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public Command(@NotNull String name) {
        this(name, new String[0]);
    }

    protected final void setCondition(@NotNull Predicate<S> condition) {
        this.condition = condition;
    }

    protected final void setDefaultExecutor(@NotNull CommandExecutor<S> executor) {
        this.defaultExecutor = executor;
    }

    @SafeVarargs
    protected final void addConditionalSyntax(@Nullable Predicate<S> condition, @NotNull CommandExecutor<S> executor,
                                              @NotNull CommandElement<S>... elements) {
        var syntax = new CommandSyntax<>(condition, executor, List.of(elements));
        this.syntaxes.add(syntax);
    }

    @SafeVarargs
    protected final void addSyntax(@NotNull CommandExecutor<S> executor, @NotNull CommandElement<S>... elements) {
        this.addConditionalSyntax(null, executor, elements);
    }

    protected final void addSubCommand(@NotNull Command<S> command) {
        this.subCommands.add(command);
    }

    public final @NotNull LiteralCommandNode<S> build() {
        return Graph.create(this).build();
    }

    public final @NotNull String getName() {
        return this.name;
    }

    public final @NotNull String[] getAliases() {
        return this.aliases;
    }

    final @Nullable Predicate<S> getCondition() {
        return this.condition;
    }

    final @Nullable CommandExecutor<S> getDefaultExecutor() {
        return this.defaultExecutor;
    }

    final @NotNull Collection<CommandSyntax<S>> getSyntaxes() {
        return this.syntaxes;
    }

    final @NotNull Collection<Command<S>> getSubCommands() {
        return this.subCommands;
    }

    protected static <S> @NotNull LiteralElement<S> literal(@NotNull String name) {
        return new LiteralElement<>(name);
    }
}
