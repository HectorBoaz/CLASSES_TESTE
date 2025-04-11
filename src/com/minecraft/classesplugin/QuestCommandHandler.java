package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class QuestCommandHandler implements CommandExecutor {
    
    private Main plugin;
    
    public QuestCommandHandler(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);
        if (data == null || data.getClassName() == null) {
            player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe primeiro!");
            return true;
        }
        
        String className = data.getClassName();
        ClassDefinition classDef = plugin.getAvailableClasses().get(className.toLowerCase());
        
        player.sendMessage(ChatColor.GOLD + "=== Missões de " + className + " ===");
        
        boolean hasActiveQuests = false;
        for (Quest quest : classDef.getQuests()) {
            if (!data.isQuestCompleted(quest.getId())) {
                hasActiveQuests = true;
                int progress = data.getQuestProgress(quest.getId());
                int target = quest.getTargetAmount();
                
                player.sendMessage(ChatColor.YELLOW + quest.getDescription() + ": " + 
                                  ChatColor.GREEN + progress + "/" + target + 
                                  ChatColor.GRAY + " (" + quest.getRewardXp() + " XP)");
            }
        }
        
        if (!hasActiveQuests) {
            player.sendMessage(ChatColor.YELLOW + "Você completou todas as missões disponíveis!");
        }
        
        return true;
    }
}