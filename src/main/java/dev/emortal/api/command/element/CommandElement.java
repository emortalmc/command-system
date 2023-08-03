package dev.emortal.api.command.element;

import com.mojang.brigadier.builder.ArgumentBuilder;
import org.jetbrains.annotations.NotNull;

public interface CommandElement<S> {

    @NotNull ArgumentBuilder<S, ?> toBuilder();
}
