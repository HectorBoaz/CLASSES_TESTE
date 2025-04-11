package com.minecraft.classesplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerClassChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    private Player player;
    private String oldClass;
    private String newClass;
    
    public PlayerClassChangeEvent(Player player, String oldClass, String newClass) {
        this.player = player;
        this.oldClass = oldClass;
        this.newClass = newClass;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getOldClass() {
        return oldClass;
    }
    
    public String getNewClass() {
        return newClass;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}