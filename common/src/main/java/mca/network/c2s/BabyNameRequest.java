package mca.network.c2s;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.entity.ai.relationship.Gender;
import mca.network.s2c.BabyNameResponse;
import mca.resources.Names;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;

public class BabyNameRequest implements Message {
    @Serial
    private static final long serialVersionUID = 4965378949498898298L;

    private final Gender gender;

    public BabyNameRequest(Gender gender) {
        this.gender = gender;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        String name = Names.pickCitizenName(gender);
        NetworkHandler.sendToPlayer(new BabyNameResponse(name), player);
    }
}
