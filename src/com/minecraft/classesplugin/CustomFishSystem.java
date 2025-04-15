package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomFishSystem {

    private final JavaPlugin plugin;
    public CustomFishSystem(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public enum FishType {
        SARDINHA(ChatColor.GREEN + "Sardinha", Material.COD,
                0.30f, -0.002f, 5, ChatColor.WHITE, "Um peixe comum e pequeno"),

        TILAPIA(ChatColor.GREEN + "Tilápia", Material.COD,
                0.25f, -0.0015f, 8, ChatColor.WHITE, "Um peixe de águas calmas"),

        CARPA(ChatColor.YELLOW + "Carpa", Material.SALMON,
                0.15f, -0.001f, 12, ChatColor.YELLOW, "Um peixe de médio porte"),

        BAGRE(ChatColor.YELLOW + "Bagre", Material.SALMON,
                0.10f, -0.0005f, 15, ChatColor.YELLOW, "Um peixe de rio de bom tamanho"),

        TUCUNARE(ChatColor.GOLD + "Tucunaré", Material.TROPICAL_FISH,
                0.08f, -0.0003f, 20, ChatColor.GOLD, "Um belo peixe de águas tropicais"),

        ESTURJAO(ChatColor.GOLD + "Esturjão", Material.PUFFERFISH,
                0.06f, -0.0002f, 25, ChatColor.GOLD, "Um peixe raro de águas profundas"),

        PEIXE_DRAGAO(ChatColor.LIGHT_PURPLE + "Peixe-Dragão", Material.PUFFERFISH,
                0.03f, -0.0001f, 35, ChatColor.LIGHT_PURPLE, "Um peixe lendário das profundezas"),

        PEIXE_ABISSAL(ChatColor.AQUA + "Peixe-Rei", Material.HEART_OF_THE_SEA,
                0.01f, -0.00005f, 50, ChatColor.AQUA, "O maior dos peixes, diz-se que traz sorte");

        private final String displayName;
        private final Material material;
        private final float baseChance;
        private final float chanceDecreasePerLevel; // Diminui menos para peixes raros
        private final int xpReward;
        private final ChatColor color;
        private final String description;

        FishType(String displayName, Material material, float baseChance,
                 float chanceDecreasePerLevel, int xpReward, ChatColor color, String description) {
            this.displayName = displayName;
            this.material = material;
            this.baseChance = baseChance;
            this.chanceDecreasePerLevel = chanceDecreasePerLevel;
            this.xpReward = xpReward;
            this.color = color;
            this.description = description;
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

        public float getChanceDecreasePerLevel() {
            return chanceDecreasePerLevel;
        }

        public int getXpReward() {
            return xpReward;
        }

        public ChatColor getColor() {
            return color;
        }

        public String getDescription() {
            return description;
        }
    }

    public ItemStack createFishItem(FishType fishType) {
        ItemStack fish = new ItemStack(fishType.getMaterial(), 1);
        ItemMeta meta = fish.getItemMeta();

        if (meta != null) {
            // Definir nome e aparência
            meta.setDisplayName(fishType.getDisplayName());

            // Adicionar lore
            List<String> lore = new ArrayList<>();
            lore.add(fishType.getColor() + fishType.getDescription());
            lore.add(ChatColor.GRAY + "XP: " + fishType.getXpReward());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Peixe especial de Pescador");
            meta.setLore(lore);

            // Adicionar brilho para peixes raros
            if (fishType == FishType.PEIXE_DRAGAO || fishType == FishType.PEIXE_ABISSAL) {
                meta.addEnchant(Enchantment.FLAME, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Adicionar tag única para identificar o peixe
            if (plugin != null) {
                NamespacedKey key = new NamespacedKey(plugin, "fish_type");
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(key, PersistentDataType.STRING, fishType.name());
            }

            fish.setItemMeta(meta);
        }

        return fish;
    }

    /**
     * Obtém um peixe aleatório baseado no nível do pescador
     *
     * @param level Nível do pescador
     * @return ItemStack do peixe pescado
     */
    public ItemStack getRandomFish(int level) {
        // Calcular chance total
        float totalChance = 0;
        Map<FishType, Float> fishChances = new HashMap<>();

        for (FishType fishType : FishType.values()) {
            // Base chance diminui com level para peixes comuns, aumenta para raros
            float adjustedChance = fishType.getBaseChance() + (level * fishType.getChanceDecreasePerLevel());

            // Garantir que a chance nunca seja negativa
            adjustedChance = Math.max(0.001f, adjustedChance);

            // Aumentar chances para peixes raros com base no nível
            if (fishType.ordinal() >= 4) { // A partir do TUCUNARE
                float bonusChance = level * 0.0015f; // Bônus para peixes raros
                adjustedChance += bonusChance;
            }

            fishChances.put(fishType, adjustedChance);
            totalChance += adjustedChance;
        }

        float roll = (float) Math.random() * totalChance;
        float currentTotal = 0;

        for (Map.Entry<FishType, Float> entry : fishChances.entrySet()) {
            currentTotal += entry.getValue();
            if (roll < currentTotal) {
                return createFishItem(entry.getKey());
            }
        }

        // Fallback (nunca deve chegar aqui)
        return createFishItem(FishType.SARDINHA);
    }

    public FishType getFishType(ItemStack item) {
        if (item != null && item.hasItemMeta() && plugin != null) {
            ItemMeta meta = item.getItemMeta();
            NamespacedKey key = new NamespacedKey(plugin, "fish_type");
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (container.has(key, PersistentDataType.STRING)) {
                String fishTypeName = container.get(key, PersistentDataType.STRING);
                try {
                    return FishType.valueOf(fishTypeName);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        return null;
    }

    public int getFishXp(ItemStack item) {
        FishType fishType = getFishType(item);
        return fishType != null ? fishType.getXpReward() : 0;
    }
}