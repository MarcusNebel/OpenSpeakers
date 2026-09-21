package com.marcusnebel.openspeakers.network;

import com.marcusnebel.openspeakers.tile.TileEntityEmitter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> Server: Der Spieler hat im Tab "Ansagen" eine Ansage für den Ansagen-Block gewählt. */
public class MessageSetAnnouncement implements IMessage {

    private static final int MAX_SOUND_LENGTH = 128;
    private static final int MAX_LABEL_LENGTH = 96;
    /** Maximaler Abstand (im Quadrat) zwischen Spieler und Ansagen-Block, gilt als Schutz vor Missbrauch. */
    private static final double MAX_DISTANCE_SQ = 100.0D;

    private BlockPos emitterPos = BlockPos.ORIGIN;
    private String soundName = "";
    private String label = "";

    /** Wird von Forge beim Empfangen benötigt. */
    public MessageSetAnnouncement() {
    }

    public MessageSetAnnouncement(BlockPos emitterPos, String soundName, String label) {
        this.emitterPos = emitterPos;
        this.soundName = soundName;
        this.label = label;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(emitterPos.toLong());
        ByteBufUtils.writeUTF8String(buf, soundName);
        ByteBufUtils.writeUTF8String(buf, label);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        emitterPos = BlockPos.fromLong(buf.readLong());
        soundName = ByteBufUtils.readUTF8String(buf);
        label = ByteBufUtils.readUTF8String(buf);
    }

    public static class Handler implements IMessageHandler<MessageSetAnnouncement, IMessage> {
        @Override
        public IMessage onMessage(MessageSetAnnouncement message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            // Änderungen an der Welt müssen im Server-Thread passieren
            player.getServerWorld().addScheduledTask(() -> apply(message, player));
            return null;
        }

        private static void apply(MessageSetAnnouncement message, EntityPlayerMP player) {
            World world = player.world;
            BlockPos pos = message.emitterPos;

            // Der Client ist nicht vertrauenswürdig: Block muss geladen und in Reichweite sein
            if (!world.isBlockLoaded(pos) || player.getDistanceSq(pos) > MAX_DISTANCE_SQ) {
                return;
            }
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof TileEntityEmitter)) {
                return;
            }
            if (!isValidSoundName(message.soundName)) {
                return;
            }
            String label = message.label.length() > MAX_LABEL_LENGTH
                    ? message.label.substring(0, MAX_LABEL_LENGTH) : message.label;

            ((TileEntityEmitter) te).setAnnouncement(message.soundName, label);
        }

        private static boolean isValidSoundName(String name) {
            return name.length() <= MAX_SOUND_LENGTH && name.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
        }
    }
}
