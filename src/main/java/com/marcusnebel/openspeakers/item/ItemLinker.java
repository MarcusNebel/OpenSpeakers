package com.marcusnebel.openspeakers.item;

import com.marcusnebel.openspeakers.ChatUtil;
import com.marcusnebel.openspeakers.OpenSpeakers;
import com.marcusnebel.openspeakers.tile.TileEntityEmitter;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class ItemLinker extends Item {

    private static final String KEY_POS = "EmitterPos";
    private static final String KEY_DIM = "EmitterDim";

    public ItemLinker() {
        setRegistryName(OpenSpeakers.MODID, "linker");
        setUnlocalizedName(OpenSpeakers.MODID + ".linker");
        setCreativeTab(OpenSpeakers.TAB);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
    float hitX, float hitY, float hitZ, EnumHand hand) {
        Block block = world.getBlockState(pos).getBlock();
        if (block != OpenSpeakers.EMITTER && block != OpenSpeakers.SPEAKER) {
            return EnumActionResult.PASS;
        }

        if (!world.isRemote) {
            ItemStack stack = player.getHeldItem(hand);
            if (block == OpenSpeakers.EMITTER) {
                selectEmitter(player, world, stack, pos);
            } else {
                linkSpeaker(player, world, stack, pos);
            }
        }
        // SUCCESS verhindert, dass zusätzlich die Block-Aktion (onBlockActivated) ausgelöst wird
        return EnumActionResult.SUCCESS;
    }

    private void selectEmitter(EntityPlayer player, World world, ItemStack stack, BlockPos pos) {
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setLong(KEY_POS, pos.toLong());
        tag.setInteger(KEY_DIM, world.provider.getDimension());

        ChatUtil.say(player, TextFormatting.GREEN,
                "Ansagen-Block bei " + ChatUtil.fmt(pos) + " ausgewählt.");

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityEmitter) {
            int count = ((TileEntityEmitter) te).getSpeakers().size();
            ChatUtil.say(player, TextFormatting.GRAY, "Aktuell verlinkt: " + count + " Lautsprecher.");
        }
        ChatUtil.say(player, TextFormatting.YELLOW,
                "Klicke jetzt mit dem Linker auf einen Lautsprecher, um ihn zu verlinken oder die Verlinkung zu entfernen.");
    }

    private void linkSpeaker(EntityPlayer player, World world, ItemStack stack, BlockPos speakerPos) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_POS)) {
            ChatUtil.say(player, TextFormatting.RED,
                    "Kein Ansagen-Block ausgewählt. Klicke zuerst mit dem Linker auf einen Ansagen-Block.");
            return;
        }

        BlockPos emitterPos = BlockPos.fromLong(tag.getLong(KEY_POS));

        if (tag.getInteger(KEY_DIM) != world.provider.getDimension()) {
            ChatUtil.say(player, TextFormatting.RED,
                    "Der ausgewählte Ansagen-Block (" + ChatUtil.fmt(emitterPos)
                            + ") steht in einer anderen Dimension.");
            return;
        }
        if (!world.isBlockLoaded(emitterPos)) {
            ChatUtil.say(player, TextFormatting.RED,
                    "Der ausgewählte Ansagen-Block (" + ChatUtil.fmt(emitterPos) + ") ist gerade nicht geladen.");
            return;
        }

        TileEntity te = world.getTileEntity(emitterPos);
        if (!(te instanceof TileEntityEmitter)) {
            tag.removeTag(KEY_POS);
            tag.removeTag(KEY_DIM);
            ChatUtil.say(player, TextFormatting.RED,
                    "Der ausgewählte Ansagen-Block bei " + ChatUtil.fmt(emitterPos)
                            + " existiert nicht mehr. Wähle einen neuen aus.");
            return;
        }

        TileEntityEmitter emitter = (TileEntityEmitter) te;
        boolean nowLinked = emitter.toggleSpeaker(speakerPos);
        int count = emitter.getSpeakers().size();

        if (nowLinked) {
            ChatUtil.say(player, TextFormatting.GREEN,
                    "Lautsprecher bei " + ChatUtil.fmt(speakerPos) + " mit Ansagen-Block bei "
                            + ChatUtil.fmt(emitterPos) + " verlinkt. (" + count + " insgesamt)");
        } else {
            ChatUtil.say(player, TextFormatting.GOLD,
                    "Verlinkung von Lautsprecher bei " + ChatUtil.fmt(speakerPos) + " zu Ansagen-Block bei "
                            + ChatUtil.fmt(emitterPos) + " entfernt. (" + count + " insgesamt)");
        }
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }
}
