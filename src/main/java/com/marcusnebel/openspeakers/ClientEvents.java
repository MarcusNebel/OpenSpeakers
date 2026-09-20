package com.marcusnebel.openspeakers;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = OpenSpeakers.MODID)
public class ClientEvents {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
                Item.getItemFromBlock(OpenSpeakers.ANNOUNCER), 0,
                new ModelResourceLocation(OpenSpeakers.ANNOUNCER.getRegistryName(), "inventory"));

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(OpenSpeakers.SPEAKER), 0,
                new ModelResourceLocation(OpenSpeakers.SPEAKER.getRegistryName(), "inventory"));

        ModelLoader.setCustomModelResourceLocation(OpenSpeakers.LINKER, 0,
                new ModelResourceLocation(OpenSpeakers.LINKER.getRegistryName(), "inventory"));
    }
}
