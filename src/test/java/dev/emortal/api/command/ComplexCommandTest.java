package dev.emortal.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.emortal.api.command.element.ArgumentElement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ComplexCommandTest {

    private static final CommandDispatcher<DummySource> dispatcher = new CommandDispatcher<>();
    private static LiteralCommandNode<DummySource> node;

    @BeforeAll
    static void setUpNode() {
        TestCommand command = new TestCommand();
        node = command.build();
        dispatcher.getRoot().addChild(node);
    }

    @AfterAll
    static void tearDownNode() {
        node = null;
    }

    @Test
    void ensureParentTreeAsExpected() {
        assertEquals("test", node.getLiteral());
        assertEquals(4, node.getChildren().size()); // 2 syntaxes on parent + 2 sub commands
    }

    @Test
    void ensureParentSendsDefaultMessage() throws CommandSyntaxException {
        DummySource.Default source = execute("test");
        assertEquals("test-default", source.nextMessage());
    }

    @Test
    void ensureSyntax1SendsCorrectMessage() throws CommandSyntaxException {
        DummySource.Default source = execute("test syntax1");
        assertEquals("test-syntax1", source.nextMessage());
    }

    @Test
    void ensureSyntax2WithNoArgumentsDoesNotWork() {
        DummySource.Default source = DummySource.create("test");
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("test syntax2", source));
    }

    @Test
    void ensureSyntax2WithArgumentsSendsCorrectMessage() throws CommandSyntaxException {
        DummySource.Default source = execute("test syntax2 hello");
        assertEquals("test-syntax2-hello", source.nextMessage());
    }

    @Test
    void ensureSyntax2ProvidesValidSuggestions() {
        DummySource.Default source = DummySource.create("test");
        Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("test syntax2 ", source)).join();
        assertEquals("test1", suggestions.getList().get(0).getText());
        assertEquals("test2", suggestions.getList().get(1).getText());
    }

    @Test
    void ensureEmptySubSendsDefaultMessage() throws CommandSyntaxException {
        DummySource.Default source = execute("test emptysub");
        assertEquals("test-emptysub-default", source.nextMessage());
    }

    @Test
    void ensureSubWithSyntax1SendsCorrectMessage() throws CommandSyntaxException {
        DummySource.Default source = execute("test sws syntax1");
        assertEquals("test-sws-syntax1", source.nextMessage());
    }

    @Test
    void ensureSubWithSyntax2WithNoArgumentsDoesNotWork() {
        DummySource.Default source = DummySource.create("test");
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("test sws syntax2", source));
    }

    @Test
    void ensureSubWithSyntax2WithArgumentsSendsCorrectMessage() throws CommandSyntaxException {
        DummySource.Default source = execute("test sws syntax2 hello");
        assertEquals("test-sws-syntax2-hello", source.nextMessage());
    }

    @Test
    void ensureSubWithSyntax2ProvidesValidSuggestions() {
        DummySource.Default source = DummySource.create("test");
        Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("test sws syntax2 ", source)).join();
        assertEquals("sws-test1", suggestions.getList().get(0).getText());
        assertEquals("sws-test2", suggestions.getList().get(1).getText());
    }

    private static DummySource.Default execute(String command) throws CommandSyntaxException {
        DummySource.Default source = DummySource.create("test");
        dispatcher.execute(command, source);
        source.await();
        return source;
    }

    private static final class TestCommand extends Command<DummySource> {

        static final Predicate<DummySource> CONDITION = source -> true;

        TestCommand() {
            super("test");

            super.setCondition(CONDITION);
            super.setDefaultExecutor(context -> context.getSource().sendMessage("test-default"));

            super.addSyntax(context -> context.getSource().sendMessage("test-syntax1"), literal("syntax1"));

            var argument = new ArgumentElement<DummySource, String>("argument1", StringArgumentType.string(), (context, builder) -> {
                builder.suggest("test1");
                builder.suggest("test2");
                return builder.buildFuture();
            });
            super.addSyntax(context -> context.getSource().sendMessage("test-syntax2-" + context.getArgument("argument1", String.class)), literal("syntax2"), argument);

            super.addSubCommand(new EmptySub());
            super.addSubCommand(new SubWithSyntax());
        }

        private static final class EmptySub extends Command<DummySource> {

            EmptySub() {
                super("emptysub");
                super.setDefaultExecutor(context -> context.getSource().sendMessage("test-emptysub-default"));
            }
        }

        private static final class SubWithSyntax extends Command<DummySource> {

            SubWithSyntax() {
                super("sws");
                super.setDefaultExecutor(context -> context.getSource().sendMessage("test-sws-default"));

                super.addSyntax(context -> context.getSource().sendMessage("test-sws-syntax1"), literal("syntax1"));

                var argument = new ArgumentElement<DummySource, String>("argument1", StringArgumentType.string(), (context, builder) -> {
                    builder.suggest("sws-test1");
                    builder.suggest("sws-test2");
                    return builder.buildFuture();
                });
                super.addSyntax(context -> context.getSource().sendMessage("test-sws-syntax2-" + context.getArgument("argument1", String.class)), literal("syntax2"), argument);
            }
        }
    }
}
