package com.marcusnebel.openspeakers.block;

import com.marcusnebel.openspeakers.ChatUtil;
import com.marcusnebel.openspeakers.OpenSpeakers;
import com.marcusnebel.openspeakers.network.MessageOpenEmitterGui;
import com.marcusnebel.openspeakers.network.NetworkHandler;
import com.marcusnebel.openspeakers.tile.TileEntityEmitter;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockEmitter extends Block {

    public BlockEmitter() {
        super(Material.IRON);
        setRegistryName(OpenSpeakers.MODID, "emitter");
        setUnlocalizedName(OpenSpeakers.MODID + ".emitter");
        setCreativeTab(OpenSpeakers.TAB);
        setHardness(2.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityEmitter();
    }

    /**
     * Redstone: Beim Wechsel von "aus" zu "an" wird die gewählte Ansage an allen verlinkten Lautsprechern
     * abgespielt. Dass das Signal wieder ausgeht, löst nichts aus (wie beim Notenblock).
     */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.isRemote) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityEmitter)) {
            return;
        }
        TileEntityEmitter emitter = (TileEntityEmitter) te;

        boolean risingEdge = emitter.updatePowered(world.isBlockPowered(pos));
        if (risingEdge) {
            emitter.pruneMissingSpeakers(world);
            emitter.playSpeakers(world);
        }
    }

    /**
     * Rechtsklick (ohne Linker): GUI öffnen.
     * Shift + Rechtsklick mit leerer Hand: gewählte Ansage an allen verlinkten Lautsprechern abspielen
     * (wie bei Redstone).
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityEmitter)) {
            return true;
        }
        TileEntityEmitter emitter = (TileEntityEmitter) te;

        int removed = emitter.pruneMissingSpeakers(world);

        if (player.isSneaking()) {
            playAnnouncement(world, player, emitter);
        } else if (player instanceof EntityPlayerMP) {
            NetworkHandler.CHANNEL.sendTo(
                    new MessageOpenEmitterGui(pos, emitter.getSpeakers(),
                            emitter.getSoundName(), emitter.getAnnouncementLabel()),
                    (EntityPlayerMP) player);
        }

        if (removed > 0) {
            ChatUtil.say(player, TextFormatting.RED, removed
                    + " Verlinkung(en) entfernt, weil der Lautsprecher nicht mehr existiert.");
        }
        return true;
    }

    private void playAnnouncement(World world, EntityPlayer player, TileEntityEmitter emitter) {
        if (!emitter.hasAnnouncement()) {
            ChatUtil.say(player, TextFormatting.RED,
                    "Keine Ansage ausgewählt. Wähle sie im Tab \"Ansagen\" der GUI aus.");
            return;
        }
        if (emitter.getSpeakers().isEmpty()) {
            ChatUtil.say(player, TextFormatting.RED, "Keine Lautsprecher verlinkt, es gibt nichts abzuspielen.");
            return;
        }
        int count = emitter.playSpeakers(world);
        ChatUtil.say(player, TextFormatting.GREEN, "Ansage \"" + emitter.getAnnouncementLabel()
                + "\" an " + count + " Lautsprecher(n) abgespielt.");
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }
}
