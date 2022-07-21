package mca.network.c2s;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.network.s2c.SkinListResponse;
import mca.resources.ClothingList;
import mca.resources.HairList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;

public class SkinListRequest implements Message {
    @Serial
    private static final long serialVersionUID = -6508206556519152120L;

    @Override
    public void receive(ServerPlayerEntity player) {
        NetworkHandler.sendToPlayer(new SkinListResponse(ClothingList.getInstance().clothing, HairList.getInstance().hair), player);
    }
}
