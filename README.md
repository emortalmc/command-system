# Command system

This is a wrapper around Mojang's Brigadier library that is designed in the style of the Minestom command API,
to allow for better written and more readable command definitions.

## Usage

As with the Minestom command system, you extend the `Command` class. You do need to provide this with the command
source you are using. In Velocity, for example, this is `CommandSource`.
You can then define syntaxes and sub commands.

Syntaxes are an easy way to define a chain of sub commands without having to represent each level with a class.
For example, you could define the command `/test a b c` with one line,
using `addSyntax(executor, literal("a"), literal("b"), literal("c"))`.

Sub commands are aimed at commands that have wider trees, such as having 4 or more sub commands for each first level
sub command, for example `/test a b`, `/test a c`, `/test a d`, etc. Using sub commands for this allows you to have
a common default executor and a common requirement shared between many second level sub commands.

### Elements

Elements are designed for use in syntaxes. In the actual Minestom command API, the `Argument` type is used to
represent different argument types, including literals and rich arguments. However, as Brigadier treats literals
as separate from arguments, we decided to continue to separate them in this API.

In addition, the element system means we only need to define one class for all arguments, as we just wrap the
argument type and suggestion provider in the element, rather than having to define a new class for each argument type.
This also means that all existing custom Brigadier argument types can be used with this API without modification.

There is a helper available to subclasses of the `Command` class called `literal` that creates a new `LiteralElement`
instance. This was provided to shorten the length of the code required to define a syntax, as it can have a tendency
to go off the screen.

This API also supports conditional syntaxes, just like Minestom, which can be defined using `addConditionalSyntax`.

### Command execution

Command parsing and execution is entirely handled by Brigadier, so you do not need to worry about that.

All commands are, by default, executed on virtual threads, so you do not need to worry about blocking, futures, or
callbacks, and can write the much cleaner blocking code in your commands without worrying whether it will actually
block any game or network threads.

The only change made to the API from Brigadier is that the `CommandExecutor` interface returns `void` rather than
returning `int`, as in my experience, the return value is ignored by most command handlers, and it only serves to
turn what would be a clean one liner in to a messy 2 lines, and forces you to put `return 1` everywhere.
