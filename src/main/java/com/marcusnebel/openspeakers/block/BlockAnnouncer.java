package com.marcusnebel.openspeakers.block;

import com.marcusnebel.openspeakers.ChatUtil;
import com.marcusnebel.openspeakers.OpenSpeakers;
import com.marcusnebel.openspeakers.tile.TileEntityAnnouncer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class BlockAnnouncer extends Block {

    public BlockAnnouncer() {
        super(Material.IRON);
        setRegistryName(OpenSpeakers.MODID, "announcer");
        setUnlocalizedName(OpenSpeakers.MODID + ".announcer");
        setCreativeTab(OpenSpeakers.TAB);
        setHardness(2.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityAnnouncer();
    }

    /**
     * Redstone: Beim Wechsel von "aus" zu "an" wird der Sound an allen verlinkten Lautsprechern abgespielt.
     * Dass das Signal wieder ausgeht, löst nichts aus.
     */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.isRemote) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityAnnouncer)) {
            return;
        }
        TileEntityAnnouncer announcer = (TileEntityAnnouncer) te;

        boolean risingEdge = announcer.updatePowered(world.isBlockPowered(pos));
        if (risingEdge) {
            announcer.pruneMissingSpeakers(world);
            announcer.playSpeakers(world);
        }
    }

    /**
     * Rechtsklick (ohne Linker): verlinkte Lautsprecher im Chat anzeigen.
     * Shift + Rechtsklick mit leerer Hand: Sound an allen verlinkten Lautsprechern abspielen (wie bei Redstone).
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityAnnouncer)) {
            return true;
        }
        TileEntityAnnouncer announcer = (TileEntityAnnouncer) te;

        int removed = announcer.pruneMissingSpeakers(world);

        if (player.isSneaking()) {
            playTestSound(world, player, announcer);
        } else {
            listSpeakers(player, pos, announcer.getSpeakers());
        }

        if (removed > 0) {
            ChatUtil.say(player, TextFormatting.RED, removed
                    + " Verlinkung(en) entfernt, weil der Lautsprecher nicht mehr existiert.");
        }
        return true;
    }

    private void listSpeakers(EntityPlayer player, BlockPos pos, List<BlockPos> speakers) {
        ChatUtil.say(player, TextFormatting.GOLD, "Ansagen-Block bei " + ChatUtil.fmt(pos) + ": "
                + speakers.size() + " Lautsprecher verlinkt");
        int i = 1;
        for (BlockPos p : speakers) {
            ChatUtil.say(player, TextFormatting.GRAY, "  " + i + ". Lautsprecher bei " + ChatUtil.fmt(p));
            i++;
        }
    }

    private void playTestSound(World world, EntityPlayer player, TileEntityAnnouncer announcer) {
        if (announcer.getSpeakers().isEmpty()) {
            ChatUtil.say(player, TextFormatting.RED, "Keine Lautsprecher verlinkt, es gibt nichts abzuspielen.");
            return;
        }
        int count = announcer.playSpeakers(world);
        ChatUtil.say(player, TextFormatting.GREEN, "Sound an " + count + " Lautsprecher(n) abgespielt.");
    }
}
