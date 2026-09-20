package com.marcusnebel.openspeakers;

import com.marcusnebel.openspeakers.block.BlockAnnouncer;
import com.marcusnebel.openspeakers.block.BlockSpeaker;
import com.marcusnebel.openspeakers.item.ItemLinker;
import com.marcusnebel.openspeakers.tile.TileEntityAnnouncer;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = OpenSpeakers.MODID, name = OpenSpeakers.NAME, version = OpenSpeakers.VERSION, acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber(modid = OpenSpeakers.MODID)
public class OpenSpeakers {
    public static final String MODID = "openspeakers";
    public static final String NAME = "OpenSpeakers";
    public static final String VERSION = "1.0";

    // Reihenfolge wichtig: TAB muss vor ANNOUNCER stehen, weil der Block-Konstruktor darauf zugreift
    public static final CreativeTabs TAB = new OpenSpeakersTab();

    public static final Block ANNOUNCER = new BlockAnnouncer();
    public static final Block SPEAKER = new BlockSpeaker();
    public static final Item LINKER = new ItemLinker();

    public static final SoundEvent TEST_SOUND =
            new SoundEvent(new ResourceLocation(MODID, "test")).setRegistryName(MODID, "test");

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(ANNOUNCER, SPEAKER);
        GameRegistry.registerTileEntity(TileEntityAnnouncer.class, new ResourceLocation(MODID, "announcer"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new ItemBlock(ANNOUNCER).setRegistryName(ANNOUNCER.getRegistryName()),
                new ItemBlock(SPEAKER).setRegistryName(SPEAKER.getRegistryName())
        );
        event.getRegistry().register(LINKER);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(TEST_SOUND);
    }
}
