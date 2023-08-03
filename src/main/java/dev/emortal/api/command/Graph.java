package dev.emortal.api.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.emortal.api.command.element.CommandElement;
import dev.emortal.api.command.element.LiteralElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

record Graph<S>(@NotNull Node<S> root) {

    static <S> @NotNull Graph<S> create(@NotNull Command<S> command) {
        return new Graph<>(Node.command(command));
    }

    private static <S> @NotNull CommandElement<S> commandToElement(@NotNull Command<S> command) {
        return new LiteralElement<>(command.getName());
    }

    @NotNull LiteralCommandNode<S> build() {
        CommandNode<S> node = this.root.build();
        if (!(node instanceof LiteralCommandNode<S> literalNode)) {
            throw new IllegalStateException("Root node is somehow not a literal node. This should be impossible.");
        }
        return literalNode;
    }

    record Node<S>(@NotNull CommandElement<S> element, @Nullable Execution<S> execution, @NotNull List<Node<S>> children) {

        static <S> @NotNull Node<S> command(@NotNull Command<S> command) {
            return ConversionNode.fromCommand(command).toNode();
        }

        @NotNull CommandNode<S> build() {
            ArgumentBuilder<S, ?> builder = this.element.toBuilder();
            if (this.execution != null) this.execution.addToBuilder(builder);

            for (Node<S> child : this.children) {
                builder.then(child.build());
            }

            return builder.build();
        }
    }

    record Execution<S>(@NotNull Predicate<S> predicate, @Nullable CommandExecutor<S> defaultExecutor, @Nullable CommandExecutor<S> executor,
                        @Nullable Predicate<S> condition) implements Predicate<S> {

        static <S> @NotNull Execution<S> fromCommand(@NotNull Command<S> command) {
            CommandExecutor<S> defaultExecutor = command.getDefaultExecutor();
            Predicate<S> defaultCondition = command.getCondition();

            CommandExecutor<S> executor = defaultExecutor;
            Predicate<S> condition = defaultCondition;
            for (CommandSyntax<S> syntax : command.getSyntaxes()) {
                if (!syntax.elements().isEmpty()) continue;
                executor = syntax.executor();
                condition = syntax.condition();
                break;
            }

            return new Execution<>(source -> defaultCondition == null || defaultCondition.test(source), defaultExecutor, executor, condition);
        }

        static <S> @NotNull Execution<S> fromSyntax(@NotNull CommandSyntax<S> syntax) {
            CommandExecutor<S> executor = syntax.executor();
            Predicate<S> condition = syntax.condition();
            return new Execution<>(source -> condition == null || condition.test(source), null, executor, condition);
        }

        @Override
        public boolean test(@NotNull S source) {
            return this.predicate.test(source);
        }

        void addToBuilder(@NotNull ArgumentBuilder<S, ?> builder) {
            if (this.condition != null) builder.requires(this.condition);
            if (this.executor != null) {
                builder.executes(convertExecutor(this.executor));
            } else if (this.defaultExecutor != null) {
                builder.executes(convertExecutor(this.defaultExecutor));
            }
        }

        private static <S> com.mojang.brigadier.@NotNull Command<S> convertExecutor(@NotNull CommandExecutor<S> executor) {
            return context -> {
                Thread.startVirtualThread(() -> executor.execute(context));
                return 1;
            };
        }
    }

    private record ConversionNode<S>(@NotNull CommandElement<S> element, @Nullable Execution<S> execution,
                                     @NotNull Map<CommandElement<S>, ConversionNode<S>> nextMap) {

        static <S> @NotNull ConversionNode<S> fromCommand(@NotNull Command<S> command) {
            ConversionNode<S> root = new ConversionNode<>(commandToElement(command), Execution.fromCommand(command));

            for (CommandSyntax<S> syntax : command.getSyntaxes()) {
                ConversionNode<S> syntaxNode = root;

                for (CommandElement<S> element : syntax.elements()) {
                    boolean last = element == syntax.elements().get(syntax.elements().size() - 1);
                    syntaxNode = syntaxNode.nextMap.computeIfAbsent(element, e -> {
                        Execution<S> execution = last ? Execution.fromSyntax(syntax) : null;
                        return new ConversionNode<>(e, execution);
                    });
                }
            }

            for (Command<S> subCommand : command.getSubCommands()) {
                root.nextMap.put(commandToElement(subCommand), fromCommand(subCommand));
            }

            return root;
        }

        ConversionNode(@NotNull CommandElement<S> element, @Nullable Execution<S> execution) {
            this(element, execution, new LinkedHashMap<>());
        }

        private Node<S> toNode() {
            @SuppressWarnings("unchecked") // this is fine - we only put Node<S> in to this array
            Node<S>[] nodes = (Node<S>[]) new Node<?>[this.nextMap.size()];

            int i = 0;
            for (ConversionNode<S> entry : this.nextMap.values()) {
                nodes[i++] = entry.toNode();
            }

            return new Node<>(this.element, this.execution, List.of(nodes));
        }
    }
}
