package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ClassCommandHandler implements CommandExecutor {

    private Main plugin;

    public ClassCommandHandler(Main plugin) {
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

        if (args.length == 0) {
            showClassInfo(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("escolher") || args[0].equalsIgnoreCase("choose")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Use: /classe escolher [nome]");
                return true;
            }

            String className = args[1].toLowerCase();
            if (!plugin.getAvailableClasses().containsKey(className)) {
                player.sendMessage(ChatColor.RED + "Classe inválida! Escolha entre: minerador, cacador, pescador, ferreiro");
                return true;
            }

            // Verifica se já tem uma classe
            PlayerClassData data = plugin.getPlayerClasses().get(playerId);
            if (data != null && data.getClassName() != null) {
                // Confirmar mudança de classe

                // Armazenar a escolha temporariamente
                plugin.getServer().getPluginManager().callEvent(
                        new PlayerClassChangeEvent(player, data.getClassName(), className));

                return true;
            }

            // Primeira vez escolhendo uma classe
            plugin.setPlayerClass(playerId, className);
            player.sendMessage(ChatColor.GREEN + "Você agora é um " +
                    plugin.getAvailableClasses().get(className).getName() + "!");

            return true;
        }

        if (args[0].equalsIgnoreCase("confirmar") || args[0].equalsIgnoreCase("confirm")) {
            // Implementar lógica para confirmar mudança de classe
            // Você precisaria adicionar um sistema de confirmações
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            showClassInfo(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("missoes") || args[0].equalsIgnoreCase("quests")) {
            showQuestInfo(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("habilidades") || args[0].equalsIgnoreCase("skills")) {
            showSkillsInfo(player);
            return true;
        }

        // Se chegou aqui, comando desconhecido
        player.sendMessage(ChatColor.RED + "Comando desconhecido. Use /classe para ajuda.");
        return true;
    }

    private void showClassInfo(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            player.sendMessage(ChatColor.YELLOW + "Você ainda não escolheu uma classe.");
            player.sendMessage(ChatColor.YELLOW + "Use /classe escolher [nome] para selecionar sua classe.");
            player.sendMessage(ChatColor.AQUA + "Classes disponíveis: Minerador, Caçador, Pescador, Guerreiro, Ferreiro, Artesão");
            return;
        }

        String className = data.getClassName();
        int level = data.getLevel();
        int xp = data.getXp();

        ClassDefinition classDef = plugin.getAvailableClasses().get(className.toLowerCase());
        int nextLevelXp = classDef.getLevelRequirement(level + 1);

        player.sendMessage(ChatColor.GOLD + "=== Informações de Classe ===");
        player.sendMessage(ChatColor.GREEN + "Classe: " + ChatColor.YELLOW + className);
        player.sendMessage(ChatColor.GREEN + "Nível: " + ChatColor.YELLOW + level);
        player.sendMessage(ChatColor.GREEN + "XP: " + ChatColor.YELLOW + xp +
                (nextLevelXp > 0 ? "/" + nextLevelXp : " (Nível máximo)"));

        player.sendMessage(ChatColor.YELLOW + "Use /classe missoes para ver suas missões atuais.");
        player.sendMessage(ChatColor.YELLOW + "Use /classe habilidades para ver suas habilidades.");
    }

    private void showQuestInfo(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe primeiro!");
            return;
        }

        String className = data.getClassName();
        ClassDefinition classDef = plugin.getAvailableClasses().get(className.toLowerCase());

    }

    private void showSkillsInfo(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe primeiro!");
            return;
        }

        String className = data.getClassName();
        int level = data.getLevel();
        ClassDefinition classDef = plugin.getAvailableClasses().get(className.toLowerCase());

        player.sendMessage(ChatColor.GOLD + "=== Habilidades de " + className + " ===");

        for (int i = 1; i <= level; i++) {
            List<String> perms = classDef.getLevelPermissions(i);
            if (perms != null && !perms.isEmpty()) {
                for (String perm : perms) {
                    player.sendMessage(ChatColor.GREEN + "✓ " + ChatColor.YELLOW +
                            "Nível " + i + ": " + getPermissionDescription(perm));
                }
            }
        }

        // Mostrar próximas habilidades
        player.sendMessage(ChatColor.GOLD + "=== Próximas Habilidades ===");

        boolean hasNextSkills = false;
        for (int i = level + 1; i <= level + 3; i++) {
            List<String> perms = classDef.getLevelPermissions(i);
            if (perms != null && !perms.isEmpty()) {
                hasNextSkills = true;
                for (String perm : perms) {
                    player.sendMessage(ChatColor.RED + "✗ " + ChatColor.GRAY +
                            "Nível " + i + ": " + getPermissionDescription(perm));
                }
            }
        }

        if (!hasNextSkills) {
            player.sendMessage(ChatColor.GRAY + "Não há mais habilidades para desbloquear.");
        }
    }

    private String getPermissionDescription(String permission) {
        // Traduzir permissões técnicas para descrições amigáveis
        if (permission.equals("classes.minerador.carvao")) {
            return "Capacidade de minerar carvão";
        } else if (permission.equals("classes.minerador.ferro")) {
            return "Capacidade de minerar ferro";
        } else if (permission.equals("classes.minerador.ouro")) {
            return "Capacidade de minerar ouro";
        } else if (permission.equals("classes.minerador.redstone")) {
            return "Capacidade de minerar redstone";
        } else if (permission.equals("classes.minerador.lapislazuli")) {
            return "Capacidade de minerar lápis-lazúli";
        } else if (permission.equals("classes.minerador.diamante")) {
            return "Capacidade de minerar diamante";
        } else if (permission.equals("classes.minerador.netherite")) {
            return "Capacidade de minerar netherite";
        }

        // Adicione mais traduções conforme necessário para outras classes

        return permission;  // Retornar a própria permissão se não houver tradução
    }
}