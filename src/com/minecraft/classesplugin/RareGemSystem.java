package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Sistema de gemas raras para mineradores
 */
public class RareGemSystem {

    private final JavaPlugin plugin;

    // Coordenadas de blocos já minerados para prevenir exploits
    private final Map<String, BlockMiningData> minedBlocks = new HashMap<>();

    // Tempo que um bloco fica registrado como "minerado" (em milissegundos)
    private static final long BLOCK_COOLDOWN = 3600000; // 1 hora

    /**
     * Construtor do sistema de gemas raras
     * @param plugin Referência ao plugin principal
     */
    public RareGemSystem(JavaPlugin plugin) {
        this.plugin = plugin;

        // Tarefa para limpar blocos antigos do registro
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                () -> cleanupMinedBlocks(),
                12000L, 12000L); // A cada 10 minutos (12000 ticks)
    }

    // Classe para armazenar dados detalhados sobre a mineração
    private class BlockMiningData {
        private final long timestamp;
        private final boolean wasNaturalBlock;

        public BlockMiningData(long timestamp, boolean wasNaturalBlock) {
            this.timestamp = timestamp;
            this.wasNaturalBlock = wasNaturalBlock;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean wasNaturalBlock() {
            return wasNaturalBlock;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > BLOCK_COOLDOWN;
        }
    }

    /**
     * Limpa blocos antigos do registro para economizar memória
     */
    private void cleanupMinedBlocks() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, BlockMiningData>> iterator = minedBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BlockMiningData> entry = iterator.next();
            if (currentTime - entry.getValue().getTimestamp() > BLOCK_COOLDOWN) {
                iterator.remove();
            }
        }
    }

    /**
     * Enum para os tipos de gemas raras disponíveis
     */
    public enum GemType {
        BRUMAFRITA(ChatColor.AQUA + "Gema Brumafrita", Material.PRISMARINE_CRYSTALS,
                0.01f, 0.001f, ChatColor.AQUA, "Extraída de minérios comuns",
                "coal_ore", "iron_ore", "copper_ore"),

        RUBILUZ(ChatColor.RED + "Gema Rubiluz", Material.AMETHYST_SHARD,
                0.0005f, 0.0001f, ChatColor.LIGHT_PURPLE, "Extraída de diamantes",
                "diamond_ore"),

        CORACAO_NETHER(ChatColor.GOLD + "Gema Coração do Nether", Material.FIRE_CHARGE,
                0.00001f, 0.000005f, ChatColor.DARK_RED, "Extraída de detritos ancestrais",
                "ancient_debris");

        private final String displayName;
        private final Material material;
        private final float baseChance;
        private final float chanceIncreasePerLevel;
        private final ChatColor color;
        private final String description;
        private final String[] validOres;

        GemType(String displayName, Material material, float baseChance,
                float chanceIncreasePerLevel, ChatColor color, String description,
                String... validOres) {
            this.displayName = displayName;
            this.material = material;
            this.baseChance = baseChance;
            this.chanceIncreasePerLevel = chanceIncreasePerLevel;
            this.color = color;
            this.description = description;
            this.validOres = validOres;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getMaterial() {
            return material;
        }

        public float getBaseChance() {
            return baseChance;
        }

        public float getChanceIncreasePerLevel() {
            return chanceIncreasePerLevel;
        }

        public ChatColor getColor() {
            return color;
        }

        public String getDescription() {
            return description;
        }

        public String[] getValidOres() {
            return validOres;
        }

        public boolean isValidOre(String oreName) {
            for (String valid : validOres) {
                if (oreName.contains(valid)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Cria um item de gema rara
     * @param gemType Tipo de gema
     * @return ItemStack representando a gema
     */
    public ItemStack createGemItem(GemType gemType) {
        ItemStack gem = new ItemStack(gemType.getMaterial(), 1);
        ItemMeta meta = gem.getItemMeta();

        if (meta != null) {
            // Definir nome e aparência
            meta.setDisplayName(gemType.getDisplayName());

            // Adicionar lore
            List<String> lore = new ArrayList<>();
            lore.add(gemType.getColor() + gemType.getDescription());
            lore.add(ChatColor.GRAY + "Uma gema rara e valiosa.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Item especial de Minerador");
            meta.setLore(lore);

            // Adicionar encantamento para brilho
            meta.addEnchant(Enchantment.FLAME, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Adicionar tag única para identificar a gema
            if (plugin != null) {
                NamespacedKey key = new NamespacedKey(plugin, "gem_type");
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(key, PersistentDataType.STRING, gemType.name());
            }

            gem.setItemMeta(meta);
        }

        return gem;
    }

    /**
     * Verifica se um jogador pode encontrar uma gema ao quebrar um bloco
     * @param player Jogador que quebrou o bloco
     * @param blockType Tipo de bloco quebrado
     * @param blockLoc Localização do bloco em formato string "world,x,y,z"
     * @param level Nível do jogador na classe minerador
     * @return ItemStack da gema encontrada ou null se nenhuma for encontrada
     */
    public ItemStack tryFindGem(Player player, Material blockType, String blockLoc, int level, boolean isNaturalBlock) {
        // Verificar se o bloco foi minerado recentemente (no mesmo local)
        if (minedBlocks.containsKey(blockLoc)) {
            BlockMiningData data = minedBlocks.get(blockLoc);

            // Se o bloco não expirou ainda
            if (!data.isExpired()) {
                // Blocos naturais podem ter uma segunda chance
                if (data.wasNaturalBlock() && isNaturalBlock) {
                    // Permitir uma baixa chance para blocos naturais que foram minerados recentemente
                    if (Math.random() >= 0.9) { // apenas 10% da chance normal
                        // continua com processamento
                    } else {
                        return null;
                    }
                } else {
                    // Blocos colocados pelo jogador não têm segunda chance
                    return null;
                }
            }
        }

        // Registrar bloco como minerado
        minedBlocks.put(blockLoc, new BlockMiningData(System.currentTimeMillis(), isNaturalBlock));

        // Verificar Silk Touch (anti-exploit)
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool != null && tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            return null;
        }

        // Se não for um bloco natural, chances drasticamente reduzidas
        float chanceMultiplier = isNaturalBlock ? 1.0f : 0.05f; // 5% da chance normal para blocos colocados

        String blockName = blockType.name().toLowerCase();

        // Tentar cada tipo de gema
        for (GemType gemType : GemType.values()) {
            if (gemType.isValidOre(blockName)) {
                float chance = (gemType.getBaseChance() + (level * gemType.getChanceIncreasePerLevel())) * chanceMultiplier;

                // Limitar a chance máxima
                float maxChance = switch (gemType) {
                    case BRUMAFRITA -> 0.05f; // 5% no máximo
                    case RUBILUZ -> 0.01f;    // 1% no máximo
                    case CORACAO_NETHER -> 0.001f; // 0.1% no máximo
                };

                chance = Math.min(chance, maxChance);

                // Verificar se o jogador encontrou a gema
                if (Math.random() < chance) {
                    return createGemItem(gemType);
                }
            }
        }

        return null;
    }

    /**
     * Verifica o tipo de uma gema a partir de um ItemStack
     * @param item Item para verificar
     * @return Tipo da gema ou null se não for uma gema
     */
    public GemType getGemType(ItemStack item) {
        if (item != null && item.hasItemMeta() && plugin != null) {
            ItemMeta meta = item.getItemMeta();
            NamespacedKey key = new NamespacedKey(plugin, "gem_type");
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (container.has(key, PersistentDataType.STRING)) {
                String gemTypeName = container.get(key, PersistentDataType.STRING);
                try {
                    return GemType.valueOf(gemTypeName);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        // Verificação alternativa pelo nome do item (menos segura)
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            for (GemType gem : GemType.values()) {
                if (name.equals(gem.getDisplayName())) {
                    return gem;
                }
            }
        }

        return null;
    }
}