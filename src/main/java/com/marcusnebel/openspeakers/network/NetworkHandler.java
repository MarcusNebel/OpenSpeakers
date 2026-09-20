package com.marcusnebel.openspeakers.network;

import com.marcusnebel.openspeakers.OpenSpeakers;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(OpenSpeakers.MODID);

    /** Muss in preInit aufgerufen werden. */
    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(MessageOpenAnnouncerGui.Handler.class, MessageOpenAnnouncerGui.class, id++, Side.CLIENT);
    }
}
