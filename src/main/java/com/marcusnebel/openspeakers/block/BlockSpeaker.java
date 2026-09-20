package com.marcusnebel.openspeakers.block;

import com.marcusnebel.openspeakers.OpenSpeakers;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockSpeaker extends Block {

    public BlockSpeaker() {
        super(Material.IRON);
        setRegistryName(OpenSpeakers.MODID, "speaker");
        setUnlocalizedName(OpenSpeakers.MODID + ".speaker");
        setCreativeTab(OpenSpeakers.TAB);
        setHardness(2.0F);
    }
}
