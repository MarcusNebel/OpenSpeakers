package com.marcusnebel.openspeakers.block;

import com.marcusnebel.openspeakers.OpenSpeakers;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockAnnouncer extends Block {

    public BlockAnnouncer() {
        super(Material.IRON);
        setRegistryName(OpenSpeakers.MODID, "announcer");
        setUnlocalizedName(OpenSpeakers.MODID + ".announcer");
        setCreativeTab(OpenSpeakers.TAB);
        setHardness(2.0F);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            // Server löst aus, alle Spieler in Reichweite hören den Sound
            world.playSound(null, pos, OpenSpeakers.TEST_SOUND, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }
}
