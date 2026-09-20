package com.marcusnebel.openspeakers.tile;

import com.marcusnebel.openspeakers.OpenSpeakers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraft.util.SoundCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TileEntityAnnouncer extends TileEntity {

    private final List<BlockPos> speakers = new ArrayList<>();

    public List<BlockPos> getSpeakers() {
        return Collections.unmodifiableList(speakers);
    }

    public boolean isLinked(BlockPos pos) {
        return speakers.contains(pos);
    }

    private boolean powered;

    /**
     * Verlinkt den Lautsprecher, oder entfernt die Verlinkung, falls sie schon besteht.
     * @return true, wenn der Lautsprecher jetzt verlinkt ist; false, wenn die Verlinkung entfernt wurde
     */
    public boolean toggleSpeaker(BlockPos pos) {
        BlockPos immutable = pos.toImmutable();
        boolean nowLinked;
        if (speakers.contains(immutable)) {
            speakers.remove(immutable);
            nowLinked = false;
        } else {
            speakers.add(immutable);
            nowLinked = true;
        }
        markDirty();
        return nowLinked;
    }

    /**
     * Entfernt Verlinkungen zu Lautsprechern, die nicht mehr existieren.
     * Nicht geladene Bereiche werden übersprungen.
     * @return Anzahl der entfernten Verlinkungen
     */
    public int pruneMissingSpeakers(World world) {
        int removed = 0;
        Iterator<BlockPos> it = speakers.iterator();
        while (it.hasNext()) {
            BlockPos p = it.next();
            if (world.isBlockLoaded(p) && world.getBlockState(p).getBlock() != OpenSpeakers.SPEAKER) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            markDirty();
        }
        return removed;
    }

    /** Merkt sich den Redstone-Zustand. Gibt true zurück, wenn das Signal gerade neu angegangen ist. */
    public boolean updatePowered(boolean nowPowered) {
        boolean risingEdge = nowPowered && !powered;
        if (powered != nowPowered) {
            powered = nowPowered;
            markDirty();
        }
        return risingEdge;
    }

    /** Spielt den Sound an jedem verlinkten Lautsprecher ab. Gibt die Anzahl der Lautsprecher zurück. */
    public int playSpeakers(World world) {
        for (BlockPos p : speakers) {
            world.playSound(null, p, OpenSpeakers.TEST_SOUND, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        return speakers.size();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (BlockPos p : speakers) {
            list.appendTag(NBTUtil.createPosTag(p));
        }
        compound.setTag("Speakers", list);
        compound.setBoolean("Powered", powered);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        speakers.clear();
        NBTTagList list = compound.getTagList("Speakers", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            speakers.add(NBTUtil.getPosFromTag(list.getCompoundTagAt(i)));
        }
        powered = compound.getBoolean("Powered");
    }
}
