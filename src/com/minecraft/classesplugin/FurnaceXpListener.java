package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de XP para Ferreiros usando fornalhas
 * Concede XP ao esquentar diversos materiais
 */
public class FurnaceXpListener implements Listener {

    private final Main plugin;

    // Mapa para rastrear último ganho de XP por jogador (evitar exploit)
    private final Map<UUID, Map<Material, Long>> lastXpGainTime = new HashMap<>();
    private static final long XP_COOLDOWN = 2000; // 2 segundos

    // Mapas para armazenar valores de XP
    private final Map<Material, Integer> ironItems = new HashMap<>();
    private final Map<Material, Integer> goldItems = new HashMap<>();
    private final Map<Material, Integer> copperItems = new HashMap<>();
    private final Map<Material, Integer> toolsAndArmor = new HashMap<>();

    public FurnaceXpListener(Main plugin) {
        this.plugin = plugin;

        // Inicializar materiais de ferro - XP: 4
        ironItems.put(Material.IRON_INGOT, 4);
        ironItems.put(Material.IRON_BLOCK, 36); // 9 * 4
        ironItems.put(Material.RAW_IRON, 4);
        ironItems.put(Material.RAW_IRON_BLOCK, 36); // 9 * 4

        // Inicializar materiais de ouro - XP: 5
        goldItems.put(Material.GOLD_INGOT, 5);
        goldItems.put(Material.GOLD_BLOCK, 45); // 9 * 5
        goldItems.put(Material.RAW_GOLD, 5);
        goldItems.put(Material.RAW_GOLD_BLOCK, 45); // 9 * 5

        // Inicializar materiais de cobre - XP: 3
        copperItems.put(Material.COPPER_INGOT, 3);
        copperItems.put(Material.COPPER_BLOCK, 27); // 9 * 3
        copperItems.put(Material.RAW_COPPER, 3);
        copperItems.put(Material.RAW_COPPER_BLOCK, 27); // 9 * 3

        // Inicializar ferramentas e armaduras - XP: 2
        // Ferramentas
        toolsAndArmor.put(Material.IRON_SWORD, 2);
        toolsAndArmor.put(Material.IRON_PICKAXE, 2);
        toolsAndArmor.put(Material.IRON_AXE, 2);
        toolsAndArmor.put(Material.IRON_SHOVEL, 2);
        toolsAndArmor.put(Material.IRON_HOE, 2);

        toolsAndArmor.put(Material.GOLDEN_SWORD, 2);
        toolsAndArmor.put(Material.GOLDEN_PICKAXE, 2);
        toolsAndArmor.put(Material.GOLDEN_AXE, 2);
        toolsAndArmor.put(Material.GOLDEN_SHOVEL, 2);
        toolsAndArmor.put(Material.GOLDEN_HOE, 2);

        // Armaduras
        toolsAndArmor.put(Material.IRON_HELMET, 2);
        toolsAndArmor.put(Material.IRON_CHESTPLATE, 2);
        toolsAndArmor.put(Material.IRON_LEGGINGS, 2);
        toolsAndArmor.put(Material.IRON_BOOTS, 2);

        toolsAndArmor.put(Material.GOLDEN_HELMET, 2);
        toolsAndArmor.put(Material.GOLDEN_CHESTPLATE, 2);
        toolsAndArmor.put(Material.GOLDEN_LEGGINGS, 2);
        toolsAndArmor.put(Material.GOLDEN_BOOTS, 2);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Material material = event.getItemType();
        int amount = event.getItemAmount();

        // Verificar se é um Ferreiro
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);
        if (data == null || data.getClassName() == null || !data.getClassName().equalsIgnoreCase("ferreiro")) {
            return;
        }

        // Verificar cooldown para prevenir exploit
        if (isOnCooldown(playerId, material)) {
            return;
        }

        // Calcular XP baseado no tipo de material
        int xpGained = calculateXp(material, amount);

        // Se ganhou XP, adicionar ao jogador
        if (xpGained > 0) {
            data.addXp(xpGained);
            player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Ferreiro (Fundição)");

            // Verificar level up
            plugin.checkLevelUp(player, data);

            // Atualizar scoreboard
            plugin.updateScoreboard(player);

            // Atualizar timestamp do último ganho
            updateLastXpGainTime(playerId, material);
        }
    }

    /**
     * Calcula o XP a ser concedido com base no material e quantidade
     * @param material Tipo de material fundido
     * @param amount Quantidade extraída
     * @return Quantidade de XP a ganhar
     */
    private int calculateXp(Material material, int amount) {
        // Verificar materiais de ferro
        if (ironItems.containsKey(material)) {
            return ironItems.get(material) * amount;
        }

        // Verificar materiais de ouro
        if (goldItems.containsKey(material)) {
            return goldItems.get(material) * amount;
        }

        // Verificar materiais de cobre
        if (copperItems.containsKey(material)) {
            return copperItems.get(material) * amount;
        }

        // Verificar ferramentas e armaduras
        if (toolsAndArmor.containsKey(material)) {
            return toolsAndArmor.get(material) * amount;
        }

        // Verificar se é comida
        if (material.isEdible()) {
            return 1 * amount;
        }

        // Verificar se é tronco (qualquer tipo)
        String materialName = material.name();
        if (materialName.contains("LOG") || materialName.contains("WOOD") ||
                materialName.contains("PLANKS") || materialName.contains("STRIPPED")) {
            return 1 * amount;
        }

        // Padrão: nenhum XP
        return 0;
    }

    /**
     * Verifica se um jogador está em cooldown para um determinado material
     * @param playerId ID do jogador
     * @param material Material para verificar
     * @return true se estiver em cooldown
     */
    private boolean isOnCooldown(UUID playerId, Material material) {
        Map<Material, Long> playerCooldowns = lastXpGainTime.getOrDefault(playerId, new HashMap<>());

        if (playerCooldowns.containsKey(material)) {
            long lastTime = playerCooldowns.get(material);
            return System.currentTimeMillis() - lastTime < XP_COOLDOWN;
        }

        return false;
    }

    /**
     * Atualiza o timestamp do último ganho de XP para um jogador e material
     * @param playerId ID do jogador
     * @param material Material processado
     */
    private void updateLastXpGainTime(UUID playerId, Material material) {
        Map<Material, Long> playerCooldowns = lastXpGainTime.getOrDefault(playerId, new HashMap<>());
        playerCooldowns.put(material, System.currentTimeMillis());
        lastXpGainTime.put(playerId, playerCooldowns);
    }

    /**
     * Limpa dados de jogadores offline para economizar memória
     */
    public void cleanupData() {
        for (UUID playerId : new HashMap<>(lastXpGainTime).keySet()) {
            if (plugin.getServer().getPlayer(playerId) == null) {
                lastXpGainTime.remove(playerId);
            }
        }
    }
}