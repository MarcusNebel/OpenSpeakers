package com.marcusnebel.openspeakers.tile;

import com.marcusnebel.openspeakers.OpenSpeakers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TileEntityAnnouncer extends TileEntity {

    /** Lautstärke der Lautsprecher. Bis 1.0 hört man Ansagen in 16 Blöcken, darüber wächst die Reichweite mit. */
    private static final float SPEAKER_VOLUME = 1.0F;

    private final List<BlockPos> speakers = new ArrayList<>();
    private boolean powered;

    /** Gewählte Ansage im Format "namensraum:ereignis". */
    private String soundName = OpenSpeakers.DEFAULT_SOUND_NAME;
    /** Anzeigetext der gewählten Ansage (Pack und Name), damit die GUI sie auch ohne das Pack anzeigen kann. */
    private String announcementLabel = OpenSpeakers.DEFAULT_SOUND_LABEL;

    // ---------- Lautsprecher ----------

    public List<BlockPos> getSpeakers() {
        return Collections.unmodifiableList(speakers);
    }

    public boolean isLinked(BlockPos pos) {
        return speakers.contains(pos);
    }

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

    // ---------- Ansage ----------

    public boolean hasAnnouncement() {
        return !soundName.isEmpty();
    }

    public String getSoundName() {
        return soundName;
    }

    public String getAnnouncementLabel() {
        return announcementLabel;
    }

    public void setAnnouncement(String soundName, String label) {
        this.soundName = soundName;
        this.announcementLabel = label;
        markDirty();
    }

    // ---------- Redstone und Wiedergabe ----------

    /** Merkt sich den Redstone-Zustand. Gibt true zurück, wenn das Signal gerade neu angegangen ist. */
    public boolean updatePowered(boolean nowPowered) {
        boolean risingEdge = nowPowered && !powered;
        if (powered != nowPowered) {
            powered = nowPowered;
            markDirty();
        }
        return risingEdge;
    }

    /**
     * Spielt die gewählte Ansage an jedem verlinkten Lautsprecher ab.
     * Der Server kennt die Sounds der Contentpacks nicht (sie liegen in den Resourcepacks der Spieler),
     * deshalb wird der Sound über seinen Namen an die Spieler in Reichweite geschickt.
     * @return Anzahl der Lautsprecher, an denen abgespielt wurde
     */
    public int playSpeakers(World world) {
        if (world.isRemote || soundName.isEmpty()) {
            return 0;
        }
        MinecraftServer server = world.getMinecraftServer();
        if (server == null) {
            return 0;
        }
        float range = SPEAKER_VOLUME > 1.0F ? 16.0F * SPEAKER_VOLUME : 16.0F;
        for (BlockPos p : speakers) {
            double x = p.getX() + 0.5D;
            double y = p.getY() + 0.5D;
            double z = p.getZ() + 0.5D;
            server.getPlayerList().sendToAllNearExcept(null, x, y, z, range, world.provider.getDimension(),
                    new SPacketCustomSound(soundName, SoundCategory.BLOCKS, x, y, z, SPEAKER_VOLUME, 1.0F));
        }
        return speakers.size();
    }

    // ---------- Speichern ----------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (BlockPos p : speakers) {
            list.appendTag(NBTUtil.createPosTag(p));
        }
        compound.setTag("Speakers", list);
        compound.setBoolean("Powered", powered);
        compound.setString("Sound", soundName);
        compound.setString("SoundLabel", announcementLabel);
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
        soundName = compound.getString("Sound");
        announcementLabel = compound.getString("SoundLabel");
        if (soundName.isEmpty() || (OpenSpeakers.MODID + ":test").equals(soundName)) {
            soundName = OpenSpeakers.DEFAULT_SOUND_NAME;
            announcementLabel = OpenSpeakers.DEFAULT_SOUND_LABEL;
        }
    }
}
