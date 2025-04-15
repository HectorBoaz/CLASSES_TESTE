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
                    // Abrir a GUI de seleção de classe
                    plugin.openClassSelectionMenu(player, false);
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
                    // Se já tem classe, informar sobre o comando para resetar
                    player.sendMessage(ChatColor.YELLOW + "Você já tem uma classe: " + data.getClassName());
                    player.sendMessage(ChatColor.YELLOW + "Use /resetarclasse para trocar de classe.");
                    return true;
                }

                // Primeira vez escolhendo uma classe
                plugin.setPlayerClass(playerId, className);
                player.sendMessage(ChatColor.GREEN + "Você agora é um " +
                        plugin.getAvailableClasses().get(className).getName() + "!");

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

            if (args[0].equalsIgnoreCase("resetar") || args[0].equalsIgnoreCase("reset")) {
                // Redirecionar para o comando específico
                player.performCommand("resetarclasse");
                return true;
            }

            // Se chegou aqui, comando desconhecido
            sendHelpMessage(player);
            return true;
        }

        private void sendHelpMessage(Player player) {
            player.sendMessage(ChatColor.GOLD + "======= Sistema de Classes =======");
            player.sendMessage(ChatColor.YELLOW + "/classe escolher" + ChatColor.WHITE + " - Abre o menu de seleção de classe");
            player.sendMessage(ChatColor.YELLOW + "/classe info" + ChatColor.WHITE + " - Mostra informações da sua classe");
            player.sendMessage(ChatColor.YELLOW + "/classe missoes" + ChatColor.WHITE + " - Mostra suas missões atuais");
            player.sendMessage(ChatColor.YELLOW + "/classe habilidades" + ChatColor.WHITE + " - Lista suas habilidades");
            player.sendMessage(ChatColor.YELLOW + "/resetarclasse" + ChatColor.WHITE + " - Abre o menu para trocar de classe");
            player.sendMessage(ChatColor.GOLD + "================================");
        }

        private void showClassInfo(Player player) {
            UUID playerId = player.getUniqueId();
            PlayerClassData data = plugin.getPlayerClasses().get(playerId);

            if (data == null || data.getClassName() == null) {
                player.sendMessage(ChatColor.YELLOW + "Você ainda não escolheu uma classe.");
                player.sendMessage(ChatColor.YELLOW + "Use /classe escolher para abrir o menu de seleção de classe.");
                player.sendMessage(ChatColor.AQUA + "Classes disponíveis: Minerador, Caçador, Pescador, Ferreiro");
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

            // Se estiver em cooldown, mostrar quanto tempo falta
            if (data.isOnResetCooldown()) {
                long remainingTime = data.getRemainingResetCooldown();
                player.sendMessage(ChatColor.RED + "Cooldown de troca: " + formatTime(remainingTime));
            }

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

            player.sendMessage(ChatColor.GOLD + "=== Missões de " + className + " ===");
            player.sendMessage(ChatColor.GRAY + "Sistema de missões em desenvolvimento.");
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
                                "Nível " + i + ": " + plugin.getPermissionDescription(perm));
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
                                "Nível " + i + ": " + plugin.getPermissionDescription(perm));
                    }
                }
            }

            if (!hasNextSkills) {
                player.sendMessage(ChatColor.GRAY + "Não há mais habilidades para desbloquear.");
            }
        }

        // Método auxiliar para formatar o tempo
        private String formatTime(long millis) {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            return hours + " horas, " + (minutes % 60) + " minutos e " + (seconds % 60) + " segundos";
        }
    }