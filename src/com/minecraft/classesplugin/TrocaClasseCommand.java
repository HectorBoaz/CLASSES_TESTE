package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para trocar de classe
 */
public class TrocaClasseCommand implements CommandExecutor {

    private final Main plugin;
    private final ClassSelectionGUI gui;

    public TrocaClasseCommand(Main plugin, ClassSelectionGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;

        // Verificar se o jogador já escolheu uma classe
        if (!plugin.getPlayerClasses().containsKey(player.getUniqueId()) ||
                plugin.getPlayerClasses().get(player.getUniqueId()).getClassName() == null) {
            player.sendMessage(ChatColor.RED + "Você ainda não escolheu uma classe!");
            player.sendMessage(ChatColor.YELLOW + "Use /classe escolher para selecionar uma classe.");
            return true;
        }

        // Verificar cooldown
        PlayerClassData data = plugin.getPlayerClasses().get(player.getUniqueId());
        if (data.isOnResetCooldown()) {
            long remainingTime = data.getRemainingResetCooldown();
            player.sendMessage(ChatColor.RED + "Você precisa esperar " + formatTime(remainingTime) +
                    " para trocar de classe novamente!");
            return true;
        }

        // Abrir o menu de confirmação para trocar de classe
        gui.openTrocaClasseConfirmationMenu(player);

        return true;
    }

    // Método auxiliar para formatar o tempo
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return hours + " horas, " + (minutes % 60) + " minutos e " + (seconds % 60) + " segundos";
    }
}