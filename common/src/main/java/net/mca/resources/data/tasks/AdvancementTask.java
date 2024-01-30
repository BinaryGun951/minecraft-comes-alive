package net.mca.resources.data.tasks;

import com.google.gson.JsonObject;
import net.mca.server.world.data.Village;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.Objects;

public class AdvancementTask extends Task {
    private final String identifier;

    public AdvancementTask(String identifier) {
        super("advancement_" + identifier);
        this.identifier = identifier;
    }

    public AdvancementTask(JsonObject json) {
        this(JsonHelper.getString(json, "id"));
    }

    @Override
    public boolean isCompleted(Village village, ServerPlayerEntity player) {
        AdvancementEntry advancement = Objects.requireNonNull(player.getServer()).getAdvancementLoader().get(new Identifier(identifier));
        return player.getAdvancementTracker().getProgress(advancement).isDone();
    }
}
