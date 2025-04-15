// Crie um novo arquivo chamado GemasAdminCommand.java
package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GemasAdminCommand implements CommandExecutor {

    private final RareGemSystem gemSystem;
    private final XpMultiplierSystem xpMultiplierSystem;

    public GemasAdminCommand(RareGemSystem gemSystem, XpMultiplierSystem xpMultiplierSystem) {
        this.gemSystem = gemSystem;
        this.xpMultiplierSystem = xpMultiplierSystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("peixeadmin")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
                return true;
            }

            // Verificar permissão
            if (!player.hasPermission("classes.admin")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
                return true;
            }

            // Sintaxe: /peixeadmin <tipo> <quantidade>
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /peixeadmin <tipo> [quantidade]");
                player.sendMessage(ChatColor.YELLOW + "Tipos disponíveis: SARDINHA, TILAPIA, CARPA, BAGRE, TUCUNARE, ESTURJAO, PEIXE_DRAGAO, PEIXE_REI");
                return true;
            }

            String fishType = args[0].toUpperCase();
            int amount = 1;

            if (args.length >= 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                    amount = Math.max(1, Math.min(64, amount)); // Limitar entre 1 e 64
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Quantidade inválida. Use um número entre 1 e 64.");
                    return true;
                }
            }

            try {
                CustomFishSystem.FishType type = CustomFishSystem.FishType.valueOf(fishType);
                ItemStack fishItem = Main.customFishSystem.createFishItem(type);
                fishItem.setAmount(amount);
                player.getInventory().addItem(fishItem);
                player.sendMessage(ChatColor.GREEN + "Você recebeu " + amount + "x " + type.getDisplayName());
                return true;
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Tipo de peixe inválido. Opções disponíveis:");
                player.sendMessage(ChatColor.YELLOW + "SARDINHA, TILAPIA, CARPA, BAGRE, TUCUNARE, ESTURJAO, PEIXE_DRAGAO, PEIXE_REI");
                return true;
            }
        }

        // Verificar permissão
        if (!player.hasPermission("classes.admin")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        // Sintaxe: /gemasadmin [gema|multiplicador] <tipo> <quantidade>
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /gemasadmin [gema|multiplicador] <tipo> <quantidade>");
            return true;
        }

        String itemType = args[0].toLowerCase();
        String typeArg = args[1].toUpperCase();
        int amount = 1;

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                amount = Math.max(1, Math.min(64, amount)); // Limitar entre 1 e 64
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Quantidade inválida. Use um número entre 1 e 64.");
                return true;
            }
        }

        if (itemType.equals("gema")) {
            // Dar gema para o jogador
            try {
                RareGemSystem.GemType gemType = RareGemSystem.GemType.valueOf(typeArg);
                ItemStack gemItem = gemSystem.createGemItem(gemType);
                gemItem.setAmount(amount);
                player.getInventory().addItem(gemItem);
                player.sendMessage(ChatColor.GREEN + "Você recebeu " + amount + "x " + gemType.getDisplayName());
                return true;
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Tipo de gema inválido. Opções: BRUMAFRITA, RUBILUZ, CORACAO_NETHER");
                return true;
            }
        } else if (itemType.equals("multiplicador")) {
            // Dar multiplicador para o jogador
            try {
                XpMultiplierSystem.Rarity rarity = XpMultiplierSystem.Rarity.valueOf(typeArg);
                ItemStack multiplierItem = xpMultiplierSystem.createMultiplierItem(rarity);
                multiplierItem.setAmount(amount);
                player.getInventory().addItem(multiplierItem);
                player.sendMessage(ChatColor.GREEN + "Você recebeu " + amount + "x Multiplicador de XP " + rarity.getDisplayName());
                return true;
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Tipo de multiplicador inválido. Opções: UNCOMMON, RARE, LEGENDARY");
                return true;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Tipo de item inválido. Use 'gema' ou 'multiplicador'.");
            return true;
        }
    }
}