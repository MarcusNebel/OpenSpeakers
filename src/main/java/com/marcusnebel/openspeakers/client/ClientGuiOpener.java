package com.marcusnebel.openspeakers.client;

import com.marcusnebel.openspeakers.client.gui.GuiEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class ClientGuiOpener {

    public static void openEmitterGui(BlockPos emitterPos, List<BlockPos> speakers,
                                        String selectedSound, String selectedLabel) {
        Minecraft mc = Minecraft.getMinecraft();
        // Pakete kommen auf dem Netzwerk-Thread an, GUIs müssen im Client-Thread geöffnet werden
        mc.addScheduledTask(() ->
                mc.displayGuiScreen(new GuiEmitter(emitterPos, speakers, selectedSound, selectedLabel)));
    }
}
