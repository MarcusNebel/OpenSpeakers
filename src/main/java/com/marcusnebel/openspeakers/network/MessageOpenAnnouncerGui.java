package com.marcusnebel.openspeakers.network;

import com.marcusnebel.openspeakers.client.ClientGuiOpener;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** Server -> Client: Öffnet die Announcer-GUI und übergibt verlinkte Lautsprecher und die gewählte Ansage. */
public class MessageOpenAnnouncerGui implements IMessage {

    private BlockPos announcerPos = BlockPos.ORIGIN;
    private List<BlockPos> speakers = new ArrayList<>();
    private String soundName = "";
    private String label = "";

    /** Wird von Forge beim Empfangen benötigt. */
    public MessageOpenAnnouncerGui() {
    }

    public MessageOpenAnnouncerGui(BlockPos announcerPos, List<BlockPos> speakers, String soundName, String label) {
        this.announcerPos = announcerPos;
        this.speakers = new ArrayList<>(speakers);
        this.soundName = soundName;
        this.label = label;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(announcerPos.toLong());
        buf.writeInt(speakers.size());
        for (BlockPos p : speakers) {
            buf.writeLong(p.toLong());
        }
        ByteBufUtils.writeUTF8String(buf, soundName);
        ByteBufUtils.writeUTF8String(buf, label);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        announcerPos = BlockPos.fromLong(buf.readLong());
        int count = buf.readInt();
        speakers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            speakers.add(BlockPos.fromLong(buf.readLong()));
        }
        soundName = ByteBufUtils.readUTF8String(buf);
        label = ByteBufUtils.readUTF8String(buf);
    }

    public static class Handler implements IMessageHandler<MessageOpenAnnouncerGui, IMessage> {
        @Override
        public IMessage onMessage(MessageOpenAnnouncerGui message, MessageContext ctx) {
            // Client-Code steckt in einer eigenen Klasse, damit dieser Handler auch auf dem Server geladen werden kann
            ClientGuiOpener.openAnnouncerGui(message.announcerPos, message.speakers, message.soundName, message.label);
            return null;
        }
    }
}
