package com.minecraft.classesplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Sistema de multiplicador de XP para o plugin de classes
 * Esta classe gerencia o sistema de multiplicadores de XP que podem ser obtidos
 * através da pesca e usados por qualquer classe para aumentar temporariamente o XP ganho.
 */
public class XpMultiplierSystem implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, MultiplierData> activeMultipliers = new HashMap<>();

    /**
     * Construtor do sistema de multiplicador de XP
     *
     * @param plugin Referência ao plugin principal
     */
    public XpMultiplierSystem(JavaPlugin plugin) {
        this.plugin = plugin;

        // Registrar eventos
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Iniciar tarefa para verificar multiplicadores expirados
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkExpiredMultipliers, 20L, 20L);
    }

    /**
     * Enum para definir as raridades dos multiplicadores
     */
    public enum Rarity {
        UNCOMMON(1.5f, 5 * 60, ChatColor.GREEN + "Incomum", Material.TROPICAL_FISH),
        RARE(1.75f, 10 * 60, ChatColor.BLUE + "Raro", Material.PUFFERFISH),
        LEGENDARY(2.0f, 20 * 60, ChatColor.GOLD + "Lendário", Material.HEART_OF_THE_SEA);

        private final float multiplier;
        private final int durationSeconds;
        private final String displayName;
        private final Material material;

        Rarity(float multiplier, int durationSeconds, String displayName, Material material) {
            this.multiplier = multiplier;
            this.durationSeconds = durationSeconds;
            this.displayName = displayName;
            this.material = material;
        }

        public float getMultiplier() {
            return multiplier;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getMaterial() {
            return material;
        }
    }

    /**
     * Classe para armazenar dados de multiplicadores ativos
     */
    public class MultiplierData {
        private final float multiplier;
        private final long expirationTime;

        public MultiplierData(float multiplier, int durationSeconds) {
            this.multiplier = multiplier;
            this.expirationTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        }

        public float getMultiplier() {
            return multiplier;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public long getTimeLeftMillis() {
            return Math.max(0, expirationTime - System.currentTimeMillis());
        }

        public int getTimeLeftSeconds() {
            return (int) (getTimeLeftMillis() / 1000);
        }
    }

    /**
     * Verificar e remover multiplicadores expirados
     */
    private void checkExpiredMultipliers() {
        Iterator<Map.Entry<UUID, MultiplierData>> iterator = activeMultipliers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MultiplierData> entry = iterator.next();
            UUID playerId = entry.getKey();
            MultiplierData data = entry.getValue();

            if (data.isExpired()) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Seu multiplicador de XP expirou!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f);
                    player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.02);
                }
                iterator.remove();
            }
        }
    }

    /**
     * Cria um item de multiplicador de XP
     *
     * @param rarity Raridade do multiplicador
     * @return ItemStack representando o multiplicador
     */
    public ItemStack createMultiplierItem(Rarity rarity) {
        ItemStack item = new ItemStack(rarity.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(rarity.getDisplayName() + " Multiplicador de XP");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Multiplicador: " + ChatColor.YELLOW + "x" + rarity.getMultiplier());
            lore.add(ChatColor.GRAY + "Duração: " + ChatColor.YELLOW + formatTime(rarity.getDurationSeconds()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clique com o botão direito para ativar!");
            meta.setLore(lore);

            // Adicionar brilho ao item
            item.addUnsafeEnchantment(Enchantment.values()[0], 1);
            ;
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Retorna um item de multiplicador baseado no nível do pescador
     *
     * @param level Nível do pescador
     * @return ItemStack do multiplicador ou null se não obtiver
     */
    public ItemStack getRandomMultiplierForLevel(int level) {
        // Definir chances base
        double uncommonChance = 0.05; // 5% base
        double rareChance = 0.02;     // 2% base
        double legendaryChance = 0.005; // 0.5% base

        // Aumentar chances com base no nível (pequeno bônus por nível)
        uncommonChance += level * 0.003; // +0.3% por nível
        rareChance += level * 0.001;     // +0.1% por nível
        legendaryChance += level * 0.0005; // +0.05% por nível

        // Limitar as chances máximas
        uncommonChance = Math.min(uncommonChance, 0.20); // Máximo 20%
        rareChance = Math.min(rareChance, 0.10);         // Máximo 10%
        legendaryChance = Math.min(legendaryChance, 0.05); // Máximo 5%

        // Determinar qual item será pescado
        double roll = Math.random();

        if (roll < legendaryChance) {
            return createMultiplierItem(Rarity.LEGENDARY);
        } else if (roll < (legendaryChance + rareChance)) {
            return createMultiplierItem(Rarity.RARE);
        } else if (roll < (legendaryChance + rareChance + uncommonChance)) {
            return createMultiplierItem(Rarity.UNCOMMON);
        }

        return null;
    }

    /**
     * Verifica se um jogador tem um multiplicador ativo
     *
     * @param playerId UUID do jogador
     * @return true se tiver multiplicador ativo
     */
    public boolean hasActiveMultiplier(UUID playerId) {

        MultiplierData data = activeMultipliers.get(playerId);
        return data != null && !data.isExpired();


    }

    /**
     * Aplica o multiplicador ao XP ganho
     *
     * @param playerId UUID do jogador
     * @param baseXp   XP base a ser multiplicado
     * @return XP com multiplicador aplicado
     */
    public int applyMultiplier(UUID playerId, int baseXp) {
        MultiplierData data = activeMultipliers.get(playerId);
        if (data != null && !data.isExpired()) {
            return Math.round(baseXp * data.getMultiplier());
        }
        return baseXp;
    }

    /**
     * Ativa um multiplicador para um jogador
     *
     * @param player Jogador que está ativando
     * @param rarity Raridade do multiplicador
     * @return true se ativado com sucesso
     */
    public boolean activateMultiplier(Player player, Rarity rarity) {
        UUID playerId = player.getUniqueId();

        // Verificar se já tem um multiplicador ativo
        if (activeMultipliers.containsKey(playerId)) {
            MultiplierData currentData = activeMultipliers.get(playerId);
            if (!currentData.isExpired()) {
                if (currentData.getMultiplier() >= rarity.getMultiplier()) {
                    player.sendMessage(ChatColor.RED + "Você já possui um multiplicador igual ou melhor ativo!");
                    return false;
                }
            }
        }

        // Ativar o novo multiplicador
        activeMultipliers.put(playerId, new MultiplierData(
                rarity.getMultiplier(),
                rarity.getDurationSeconds()));

        // Efeitos visuais e sonoros
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.05);

        // Mensagem
        player.sendMessage(ChatColor.GREEN + "Multiplicador de XP " + rarity.getDisplayName()
                + ChatColor.GREEN + " ativado!");
        player.sendMessage(ChatColor.YELLOW + "Duração: " + (rarity.getDurationSeconds() / 60)
                + " minutos. Multiplicador: x" + rarity.getMultiplier());

        return true;
    }

    /**
     * Evento para detectar uso do item multiplicador
     */
    @EventHandler
    public void onPlayerUseMultiplier(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = item.getItemMeta().getDisplayName();

        // Verificar se é um item multiplicador de XP
        if (displayName.contains("Multiplicador de XP")) {
            event.setCancelled(true);

            Rarity rarity = null;

            // Identificar a raridade com base no nome
            if (displayName.contains("Incomum")) {
                rarity = Rarity.UNCOMMON;
            } else if (displayName.contains("Raro")) {
                rarity = Rarity.RARE;
            } else if (displayName.contains("Lendário")) {
                rarity = Rarity.LEGENDARY;
            }

            if (rarity != null && activateMultiplier(player, rarity)) {
                // Remover 1 item da mão do jogador
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInMainHand(item);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            }
        }

        if (hasActiveMultiplier(player.getUniqueId())) {
            MultiplierData currentData = activeMultipliers.get(player.getUniqueId());

            // Verificar se o atual já expirou
            if (!currentData.isExpired()) {
                player.sendMessage(ChatColor.RED + "Você já possui um multiplicador de XP ativo!");
                player.sendMessage(ChatColor.RED + "Aguarde ele expirar antes de usar outro.");
                return;
            }
        }

    }

    /**
     * Formata um tempo em segundos para uma string legível
     *
     * @param seconds Tempo em segundos
     * @return String formatada
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        return minutes + " minutos";
    }
}