package com.minecraft.classesplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassCommandHandlerAdmin implements CommandExecutor {

    private Main plugin;

    public ClassCommandHandlerAdmin(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== ClassesPlugin Admin ===");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin setclasse <jogador> <classe> - Define a classe de um jogador");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin resetclasse <jogador> - Remove a classe de um jogador");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin xp <add|remove|set> <jogador> <quantidade> - Gerencia XP");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin nivel <set|add> <jogador> <quantidade> - Gerencia níveis");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin missao <complete|reset|setprogress> <jogador> <id_missao> [progresso] - Gerencia missões");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin info <jogador> - Mostra informações detalhadas");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin reload - Recarrega a configuração");
            sender.sendMessage(ChatColor.YELLOW + "/classeadmin list - Lista jogadores e classes");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Verificar permissões para o comando específico
        if (!hasPermission(sender, "classes.admin." + subCommand)) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (subCommand.equals("setclasse") && args.length == 3) {
            return handleSetClasse(sender, args[1], args[2]);
        } else if (subCommand.equals("resetclasse") && args.length == 2) {
            return handleResetClasse(sender, args[1]);
        } else if (subCommand.equals("xp") && args.length == 4) {
            return handleXP(sender, args[1], args[2], args[3]);
        } else if (subCommand.equals("nivel") && args.length == 4) {
            return handleNivel(sender, args[1], args[2], args[3]);
        } else if (subCommand.equals("missao") && args.length >= 4) {
            return handleMissao(sender, args[1], args[2], args[3], args.length > 4 ? args[4] : null);
        } else if (subCommand.equals("info") && args.length == 2) {
            return handleInfo(sender, args[1]);
        } else if (subCommand.equals("reload") && args.length == 1) {
            return handleReload(sender);
        } else if (subCommand.equals("list") && args.length == 1) {
            return handleList(sender);
        }

        sender.sendMessage(ChatColor.RED + "Uso incorreto do comando. Use /classeadmin para ajuda.");
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("classes.admin.*") || sender.isOp();
    }

    private boolean handleSetClasse(CommandSender sender, String playerName, String className) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline!");
            return true;
        }

        className = className.toLowerCase();
        if (!plugin.getAvailableClasses().containsKey(className)) {
            sender.sendMessage(ChatColor.RED + "Classe inválida! Escolha entre: minerador, cacador, pescador, ferreiro");
            return true;
        }

        UUID playerId = targetPlayer.getUniqueId();
        plugin.setPlayerClass(playerId, className);

        sender.sendMessage(ChatColor.GREEN + "Classe de " + targetPlayer.getName() + " definida para " +
                plugin.getAvailableClasses().get(className).getName() + "!");
        targetPlayer.sendMessage(ChatColor.GREEN + "Um administrador definiu sua classe para " +
                plugin.getAvailableClasses().get(className).getName() + "!");

        return true;
    }

    private boolean handleResetClasse(CommandSender sender, String playerName) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline!");
            return true;
        }

        UUID playerId = targetPlayer.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            sender.sendMessage(ChatColor.RED + "Este jogador não tem uma classe definida!");
            return true;
        }

        String oldClassName = data.getClassName();
        data.setClassName(null);
        data.setLevel(1);
        data.setXp(0);

        // Salvar mudanças
        plugin.savePlayerData(playerId);

        sender.sendMessage(ChatColor.GREEN + "Classe de " + targetPlayer.getName() + " foi resetada!");
        targetPlayer.sendMessage(ChatColor.YELLOW + "Um administrador removeu sua classe " + oldClassName + "!");

        return true;
    }

    private boolean handleXP(CommandSender sender, String action, String playerName, String amountStr) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline!");
            return true;
        }

        UUID playerId = targetPlayer.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            sender.sendMessage(ChatColor.RED + "Este jogador não tem uma classe definida!");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantidade inválida! Use um número inteiro.");
            return true;
        }

        if (action.equalsIgnoreCase("add")) {
            data.addXp(amount);
            sender.sendMessage(ChatColor.GREEN + "Adicionado " + amount + " de XP para " + targetPlayer.getName() + "!");
            targetPlayer.sendMessage(ChatColor.GREEN + "Um administrador adicionou " + amount + " de XP para você!");
        } else if (action.equalsIgnoreCase("remove")) {
            int currentXp = data.getXp();
            data.setXp(Math.max(0, currentXp - amount));
            sender.sendMessage(ChatColor.GREEN + "Removido " + Math.min(currentXp, amount) + " de XP de " + targetPlayer.getName() + "!");
            targetPlayer.sendMessage(ChatColor.YELLOW + "Um administrador removeu " + Math.min(currentXp, amount) + " de XP de você!");
        } else if (action.equalsIgnoreCase("set")) {
            data.setXp(Math.max(0, amount));
            sender.sendMessage(ChatColor.GREEN + "XP de " + targetPlayer.getName() + " definido para " + amount + "!");
            targetPlayer.sendMessage(ChatColor.YELLOW + "Um administrador definiu seu XP para " + amount + "!");
        } else {
            sender.sendMessage(ChatColor.RED + "Ação inválida! Use add, remove ou set.");
            return true;
        }

        // Verificar level up e salvar mudanças
        plugin.checkLevelUp(targetPlayer, data);
        plugin.savePlayerData(playerId);
        plugin.updateScoreboard(targetPlayer);

        return true;
    }

    private boolean handleNivel(CommandSender sender, String action, String playerName, String amountStr) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline!");
            return true;
        }

        UUID playerId = targetPlayer.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            sender.sendMessage(ChatColor.RED + "Este jogador não tem uma classe definida!");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantidade inválida! Use um número inteiro.");
            return true;
        }

        if (action.equalsIgnoreCase("set")) {
            data.setLevel(Math.max(1, amount));
            sender.sendMessage(ChatColor.GREEN + "Nível de " + targetPlayer.getName() + " definido para " + amount + "!");
            targetPlayer.sendMessage(ChatColor.YELLOW + "Um administrador definiu seu nível para " + amount + "!");
        } else if (action.equalsIgnoreCase("add")) {
            int newLevel = data.getLevel() + amount;
            data.setLevel(Math.max(1, newLevel));
            sender.sendMessage(ChatColor.GREEN + "Adicionado " + amount + " níveis para " + targetPlayer.getName() + "!");
            targetPlayer.sendMessage(ChatColor.GREEN + "Um administrador adicionou " + amount + " níveis para você!");
        } else {
            sender.sendMessage(ChatColor.RED + "Ação inválida! Use set ou add.");
            return true;
        }

        // Aplicar permissões do novo nível
        String className = data.getClassName();
        int level = data.getLevel();
        ClassDefinition classDef = plugin.getAvailableClasses().get(className.toLowerCase());

        // Notificar sobre novas permissões
        List<String> newPerms = new ArrayList<>();
        for (int i = 1; i <= level; i++) {
            List<String> levelPerms = classDef.getLevelPermissions(i);
            if (levelPerms != null) {
                newPerms.addAll(levelPerms);
            }
        }

        if (!newPerms.isEmpty()) {
            targetPlayer.sendMessage(ChatColor.GOLD + "Você agora tem acesso a novas habilidades!");
        }

        // Salvar mudanças
        plugin.savePlayerData(playerId);
        plugin.updateScoreboard(targetPlayer);

        return true;
    }

    private boolean handleMissao(CommandSender sender, String action, String playerName, String questId, String progressStr) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline!");
            return true;
        }

        UUID playerId = targetPlayer.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            sender.sendMessage(ChatColor.RED + "Este jogador não tem uma classe definida!");
            return true;
        }

        String className = data.getClassName().toLowerCase();
        ClassDefinition classDef = plugin.getAvailableClasses().get(className);

        if (action.equalsIgnoreCase("setprogress")) {
            if (progressStr == null) {
                sender.sendMessage(ChatColor.RED + "Você precisa especificar o progresso!");
                return true;
            }

            int progress;
            try {
                progress = Integer.parseInt(progressStr);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Progresso inválido! Use um número inteiro.");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Ação inválida! Use complete, reset ou setprogress.");
            return true;
        }

        // Salvar mudanças
        plugin.savePlayerData(playerId);
        plugin.updateScoreboard(targetPlayer);

        return true;
    }

    private boolean handleInfo(CommandSender sender, String playerName) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline!");
            return true;
        }

        UUID playerId = targetPlayer.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data == null || data.getClassName() == null) {
            sender.sendMessage(ChatColor.RED + targetPlayer.getName() + " não tem uma classe definida!");
            return true;
        }

        String className = data.getClassName();
        int level = data.getLevel();
        int xp = data.getXp();
        ClassDefinition classDef = plugin.getAvailableClasses().get(className.toLowerCase());
        int nextLevelXp = classDef.getLevelRequirement(level + 1);

        sender.sendMessage(ChatColor.GOLD + "=== Informações de " + targetPlayer.getName() + " ===");
        sender.sendMessage(ChatColor.GREEN + "Classe: " + ChatColor.YELLOW + className);
        sender.sendMessage(ChatColor.GREEN + "Nível: " + ChatColor.YELLOW + level);
        sender.sendMessage(ChatColor.GREEN + "XP: " + ChatColor.YELLOW + xp +
                (nextLevelXp > 0 ? "/" + nextLevelXp : " (Nível máximo)"));

        // Mostrar habilidades desbloqueadas
        sender.sendMessage(ChatColor.GOLD + "Habilidades desbloqueadas:");
        for (int i = 1; i <= level; i++) {
            List<String> perms = classDef.getLevelPermissions(i);
            if (perms != null && !perms.isEmpty()) {
                for (String perm : perms) {
                    sender.sendMessage(ChatColor.GREEN + "- Nível " + i + ": " + plugin.getPermissionDescription(perm));
                }
            }
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        // Recarregar configuração
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        sender.sendMessage(ChatColor.GREEN + "Configuração do plugin recarregada com sucesso!");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Map<UUID, PlayerClassData> playerClasses = plugin.getPlayerClasses();

        if (playerClasses.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhum jogador tem uma classe definida!");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Jogadores e Classes ===");

        for (Map.Entry<UUID, PlayerClassData> entry : playerClasses.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerClassData data = entry.getValue();

            if (data.getClassName() != null) {
                String playerName = Bukkit.getOfflinePlayer(playerId).getName();
                sender.sendMessage(ChatColor.YELLOW + playerName + ": " +
                        ChatColor.GREEN + data.getClassName() +
                        ChatColor.GRAY + " (Nível " + data.getLevel() + ", XP: " + data.getXp() + ")");
            }
        }

        return true;
    }
}