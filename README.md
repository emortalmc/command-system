# Command system

This is a wrapper around Mojang's Brigadier library that is designed in the style of the Minestom command API,
to allow for better written and more readable command definitions.

## Usage

As with the Minestom command system, you extend the `Command` class. You do need to provide this with the command
source you are using. In Velocity, for example, this is `CommandSource`.
You can then define syntax combinations and sub commands.

Syntax combinations are an easy way to define a chain of sub commands without having to represent each level
with a class. For example, you could define the command `/test a b c` with one line,
using `addSyntax(executor, literal("a"), literal("b"), literal("c"))`.

Sub commands are aimed at commands that have wider trees, such as having 4 or more sub commands for each first level
sub command, for example `/test a b`, `/test a c`, `/test a d`, etc. Using sub commands for this allows you to have
a common default executor and a common requirement shared between many second level sub commands.
