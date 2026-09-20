package com.marcusnebel.openspeakers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class ChatUtil {

    /** Schickt dem Spieler eine farbige Chatnachricht (nur serverseitig aufrufen). */
    public static void say(EntityPlayer player, TextFormatting color, String text) {
        ITextComponent component = new TextComponentString(text);
        component.getStyle().setColor(color);
        player.sendMessage(component);
    }

    /** Formatiert eine Position als "x, y, z". */
    public static String fmt(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
