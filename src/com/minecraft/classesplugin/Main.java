package com.minecraft.classesplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;


import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    //mensagem pro hector: oi

    private Map<UUID, PlayerClassData> playerClasses;
    private Map<String, ClassDefinition> availableClasses;
    private Map<String, Integer> classCounters = new HashMap<>();
    private File classCountersFile;
    private File playerDataFolder;
    private FileConfiguration config;
    private XpMultiplierSystem xpMultiplierSystem;
    private RareGemSystem gemSystem;

    // Armazena o timestamp de quando o jogador pode forjar novamente
    private final HashMap<UUID, Long> ferreiroCooldowns = new HashMap<>();

    // Substitua o mapa existente de blocos minerados
    private final Map<String, BlockMiningData> minedBlocks = new HashMap<>();
    private static final long BLOCK_COOLDOWN = 3600000; // 1 hora em milissegundos

    // Classe para armazenar dados detalhados sobre a mineração
    private class BlockMiningData {
        private final long timestamp;
        private final boolean wasNaturalBlock;

        public BlockMiningData(long timestamp, boolean wasNaturalBlock) {
            this.timestamp = timestamp;
            this.wasNaturalBlock = wasNaturalBlock;
        }

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
     * Obtém o mapa de definições de classes disponíveis
     *
     * @return Mapa de nomes de classe para ClassDefinition
     */
    public Map<String, ClassDefinition> getAvailableClasses() {
        return availableClasses;
    }

    /**
     * Obtém a descrição legível de uma permissão
     *
     * @param event A permissão técnica
     * @return Descrição legível da permissão
     */


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        block.setMetadata("player_placed", new FixedMetadataValue(this, true));
    }

    public static CustomFishSystem customFishSystem;

    @Override
    public void onEnable() {
        // Inicializa o sistema
        // Inicialização do plugin
        saveDefaultConfig();
        config = getConfig();

        getServer().getScheduler().runTaskTimer(this, () -> {
            // Limpar dados de jogadores offline
            Iterator<Map.Entry<UUID, PlayerMovementData>> it = playerMovementData.entrySet().iterator();
            while (it.hasNext()) {
                UUID playerId = it.next().getKey();
                if (getServer().getPlayer(playerId) == null) {
                    it.remove();
                }
            }
        }, 12000L, 12000L); // Verificar a cada 10 minutos

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (furnaceXpListener != null) {
                furnaceXpListener.cleanupData();
            }
        }, 12000L, 12000L); // A cada 10 minutos

        // Criar pasta para dados de jogadores
        playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }


        // Inicializar mapas
        playerClasses = new HashMap<>();
        availableClasses = new HashMap<>();
        customFishSystem = new CustomFishSystem(this);
        ClassesAPI.init(this);
        getLogger().info("Sistema de Classes API inicializada!");
        ClassSelectionGUI ClassSelectionGUI = new ClassSelectionGUI(this);

        furnaceXpListener = new FurnaceXpListener(this);
        getServer().getPluginManager().registerEvents(furnaceXpListener, this);
        getLogger().info("Sistema de XP de Fornalha para Ferreiros inicializado!");
        // ferreiroCooldowns já é inicializado na declaração

        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);


        // Inicializar o sistema de multiplicador de XP
        xpMultiplierSystem = new XpMultiplierSystem(this);
        getLogger().info("Sistema de Balanceamento de Mobs inicializado!");

        // Adicione estes campos na classe Main
        Map<String, Integer> classCounters = new HashMap<>();
        File classCountersFile;

// Adicione este método em onEnable() na classe Main, após a inicialização de outras coisas

        // Inicializar o sistema de gemas raras
        gemSystem = new RareGemSystem(this);

        // Definir classes disponíveis
        initializeClasses();
        loadClassCounters();

        // Comandos
        getCommand("classe").setExecutor(new ClassCommandHandler(this));
        getCommand("classeadmin").setExecutor(new ClassCommandHandlerAdmin(this));
        getCommand("gemasadmin").setExecutor(new GemasAdminCommand(gemSystem, xpMultiplierSystem));
        getCommand("peixeadmin").setExecutor(new GemasAdminCommand(gemSystem, xpMultiplierSystem));
        getCommand("trocaclasse").setExecutor(new TrocaClasseCommand(this, ClassSelectionGUI));


        // Agendador para efeitos visuais e atualizações
        Bukkit.getScheduler().runTaskTimer(this, () -> updateVisualEffects(), 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> checkAllToolRestrictions(), 20L, 100L);


        Bukkit.getScheduler().runTaskTimer(this, () -> checkInvalidToolsForAllPlayers(), 20L, 100L); // Verificar a cada 5 segundos (100 ticks)

        getLogger().info("Sistema de Classes ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Salvando dados de jogadores...");

        try {
            // Salvar dados dos jogadores
            for (UUID playerId : playerClasses.keySet()) {
                try {
                    savePlayerData(playerId);
                } catch (Exception e) {
                    getLogger().warning("Erro ao salvar dados para o jogador " + playerId + ": " + e.getMessage());
                }
            }

            if (furnaceXpListener != null) {
                furnaceXpListener.cleanupData();
            }

            getLogger().info("Sistema de Classes desativado. Dados salvos.");
        } catch (Exception e) {
            getLogger().severe("Erro crítico ao desativar o plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static class PlayerMovementData {
        private final Location lastLocation;
        private final long timestamp;
        private int stationaryCount;

        public PlayerMovementData(Location location) {
            this.lastLocation = location;
            this.timestamp = System.currentTimeMillis();
            this.stationaryCount = 0;
        }

        public Location getLastLocation() {
            return lastLocation;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getStationaryCount() {
            return stationaryCount;
        }

        public void incrementStationaryCount() {
            this.stationaryCount++;
        }

        public void resetStationaryCount() {
            this.stationaryCount = 0;
        }
    }



    /**
     * Verifica se um jogador está parado há muito tempo (possível AFK)
     * @param playerId UUID do jogador
     * @param currentLocation Localização atual do jogador
     * @return true se o jogador estiver "AFK"
     */
    public boolean isPlayerStationary(UUID playerId, Location currentLocation) {
        // Verificar se temos dados anteriores
        if (!playerMovementData.containsKey(playerId)) {
            playerMovementData.put(playerId, new PlayerMovementData(currentLocation));
            return false;
        }

        // Obter dados anteriores
        PlayerMovementData data = playerMovementData.get(playerId);
        Location lastLocation = data.getLastLocation();

        // Verificar se o jogador se moveu
        double distance = lastLocation.distance(currentLocation);
        boolean isStationary = distance < 0.2; // Limiar para considerar como "parado"

        // Atualizar dados
        if (isStationary) {
            data.incrementStationaryCount();
        } else {
            // Criar um novo registro com a localização atual
            playerMovementData.put(playerId, new PlayerMovementData(currentLocation));
        }

        // Verificar se o jogador ficou parado por muito tempo
        return data.getStationaryCount() > 10; // 10 verificações consecutivas
    }

    private Map<UUID, PlayerMovementData> playerMovementData = new HashMap<>();

    private void loadClassCounters() {
        classCountersFile = new File(getDataFolder(), "class_counters.yml");

        if (!classCountersFile.exists()) {
            // Inicializar contadores zerados para todas as classes
            for (String className : availableClasses.keySet()) {
                classCounters.put(className, 0);
            }
            saveClassCounters();
        } else {
            // Carregar contadores existentes
            FileConfiguration config = YamlConfiguration.loadConfiguration(classCountersFile);

            for (String className : availableClasses.keySet()) {
                classCounters.put(className, config.getInt(className, 0));
            }
        }
    }

    private void saveClassCounters() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Integer> entry : classCounters.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(classCountersFile);
        } catch (IOException e) {
            getLogger().warning("Não foi possível salvar contadores de classe: " + e.getMessage());
        }
    }

    /**
     * Obtém o mapa de dados de classe dos jogadores
     *
     * @return Mapa de UUID para PlayerClassData
     */
    public Map<UUID, PlayerClassData> getPlayerClasses() {
        return playerClasses;
    }

    // Inicialização das definições de classes
    private void initializeClasses() {
        // Classe Minerador
        ClassDefinition minerador = new ClassDefinition("Minerador");
        minerador.addLevelRequirement(1, 100);
        minerador.addLevelRequirement(2, 600);
        minerador.addLevelRequirement(3, 2000);
        minerador.addLevelRequirement(4, 5000);
        minerador.addLevelRequirement(5, 10000);
        minerador.addLevelRequirement(6, 15000);
        minerador.addLevelRequirement(7, 20000);
        minerador.addLevelRequirement(8, 30000);
        minerador.addLevelRequirement(9, 40000);
        minerador.addLevelRequirement(10, 50000);
        minerador.addLevelRequirement(11, 65000);
        minerador.addLevelRequirement(12, 80000);
        minerador.addLevelRequirement(13, 95000);
        minerador.addLevelRequirement(14, 120000);
        minerador.addLevelRequirement(15, 150000);
        minerador.addLevelRequirement(16, 200000);
        minerador.addLevelRequirement(17, 250000);
        minerador.addLevelRequirement(18, 300000);
        minerador.addLevelRequirement(19, 375000);
        minerador.addLevelRequirement(20, 450000);
        minerador.addLevelRequirement(21, 600000);
        minerador.addLevelRequirement(22, 750000);
        minerador.addLevelRequirement(23, 1000000);
        minerador.addLevelRequirement(24, 1250000);
        minerador.addLevelRequirement(25, 1500000);
        minerador.addLevelRequirement(26, 1750000);
        minerador.addLevelRequirement(27, 2000000);
        minerador.addLevelRequirement(28, 2500000);
        minerador.addLevelRequirement(29, 3000000);
        minerador.addLevelRequirement(30, 4000000);


        // Habilidades desbloqueáveis do Minerador
        minerador.addLevelPermission(1, "classes.minerador.carvao");
        minerador.addLevelPermission(4, "classes.minerador.ferro");
        minerador.addLevelPermission(8, "classes.minerador.ouro");
        minerador.addLevelPermission(14, "classes.minerador.redstone");
        minerador.addLevelPermission(17, "classes.minerador.lapislazuli");
        minerador.addLevelPermission(20, "classes.minerador.diamante");
        minerador.addLevelPermission(30, "classes.minerador.netherite");

        // Ferramentas permitidas do Minerador
        minerador.addAllowedTool(Material.WOODEN_PICKAXE);
        minerador.addAllowedTool(Material.STONE_PICKAXE);
        minerador.addAllowedTool(Material.IRON_PICKAXE);
        minerador.addAllowedTool(Material.GOLDEN_PICKAXE);
        minerador.addAllowedTool(Material.DIAMOND_PICKAXE);
        minerador.addAllowedTool(Material.NETHERITE_PICKAXE);

        // Efeitos visuais do Minerador
        minerador.setParticleEffect(1, Particle.CRIT);
        minerador.setParticleEffect(10, Particle.FLAME);
        minerador.setParticleEffect(20, Particle.PORTAL);
        minerador.setParticleEffect(30, Particle.DRAGON_BREATH);

        // Adicionar Minerador às classes disponíveis
        availableClasses.put("minerador", minerador);

        // Classe Caçador
        ClassDefinition cacador = new ClassDefinition("Caçador");
        cacador.addLevelRequirement(1, 100);
        cacador.addLevelRequirement(2, 250);
        cacador.addLevelRequirement(3, 750);
        cacador.addLevelRequirement(4, 1500);
        cacador.addLevelRequirement(5, 2500);
        cacador.addLevelRequirement(6, 3500);
        cacador.addLevelRequirement(7, 5000);
        cacador.addLevelRequirement(8, 6500);
        cacador.addLevelRequirement(9, 8000);
        cacador.addLevelRequirement(10, 10000);
        cacador.addLevelRequirement(11, 12500);
        cacador.addLevelRequirement(12, 15000);
        cacador.addLevelRequirement(13, 17500);
        cacador.addLevelRequirement(14, 20000);
        cacador.addLevelRequirement(15, 24000);
        cacador.addLevelRequirement(16, 28000);
        cacador.addLevelRequirement(17, 32000);
        cacador.addLevelRequirement(18, 38000);
        cacador.addLevelRequirement(19, 45000);
        cacador.addLevelRequirement(20, 52000);
        cacador.addLevelRequirement(21, 60000);
        cacador.addLevelRequirement(22, 70000);
        cacador.addLevelRequirement(23, 80000);
        cacador.addLevelRequirement(24, 90000);
        cacador.addLevelRequirement(25, 100000);
        cacador.addLevelRequirement(26, 115000);
        cacador.addLevelRequirement(27, 130000);
        cacador.addLevelRequirement(28, 150000);
        cacador.addLevelRequirement(29, 170000);
        cacador.addLevelRequirement(30, 200000);


        cacador.addLevelPermission(1, "classes.cacador.animais");
        cacador.addLevelPermission(3, "classes.cacador.monstros");
        cacador.addLevelPermission(7, "classes.cacador.rastrear");
        cacador.addLevelPermission(10, "classes.cacador.armadilhas");
        cacador.addLevelPermission(15, "classes.cacador.pets");
        cacador.addLevelPermission(20, "classes.cacador.bosses");

        cacador.addAllowedTool(Material.WOODEN_SWORD);
        cacador.addAllowedTool(Material.STONE_SWORD);
        cacador.addAllowedTool(Material.IRON_SWORD);
        cacador.addAllowedTool(Material.GOLDEN_SWORD);
        cacador.addAllowedTool(Material.DIAMOND_SWORD);
        cacador.addAllowedTool(Material.NETHERITE_SWORD);
        cacador.addAllowedTool(Material.BOW);
        cacador.addAllowedTool(Material.CROSSBOW);

        cacador.setParticleEffect(1, Particle.CLOUD);
        cacador.setParticleEffect(10, Particle.SMOKE);
        cacador.setParticleEffect(20, Particle.SOUL);

        availableClasses.put("cacador", cacador);

        // Classe Pescador
        ClassDefinition pescador = new ClassDefinition("Pescador");
        pescador.addLevelRequirement(1, 100);
        pescador.addLevelRequirement(2, 375);
        pescador.addLevelRequirement(3, 1100);
        pescador.addLevelRequirement(4, 2600);
        pescador.addLevelRequirement(5, 4500);
        pescador.addLevelRequirement(6, 6000);
        pescador.addLevelRequirement(7, 7500);
        pescador.addLevelRequirement(8, 9000);
        pescador.addLevelRequirement(9, 11000);
        pescador.addLevelRequirement(10, 13000);
        pescador.addLevelRequirement(11, 15000);
        pescador.addLevelRequirement(12, 18000);
        pescador.addLevelRequirement(13, 21000);
        pescador.addLevelRequirement(14, 25000);
        pescador.addLevelRequirement(15, 29000);
        pescador.addLevelRequirement(16, 33000);
        pescador.addLevelRequirement(17, 38000);
        pescador.addLevelRequirement(18, 45000);
        pescador.addLevelRequirement(19, 52000);
        pescador.addLevelRequirement(20, 60000);
        pescador.addLevelRequirement(21, 68000);
        pescador.addLevelRequirement(22, 77000);
        pescador.addLevelRequirement(23, 87000);
        pescador.addLevelRequirement(24, 97000);
        pescador.addLevelRequirement(25, 110000);
        pescador.addLevelRequirement(26, 125000);
        pescador.addLevelRequirement(27, 140000);
        pescador.addLevelRequirement(28, 160000);
        pescador.addLevelRequirement(29, 180000);
        pescador.addLevelRequirement(30, 200000);


        pescador.addLevelPermission(1, "classes.pescador.peixes");
        pescador.addLevelPermission(5, "classes.pescador.tesouros");
        pescador.addLevelPermission(10, "classes.pescador.raros");
        pescador.addLevelPermission(15, "classes.pescador.locaisespeciais");
        pescador.addLevelPermission(20, "classes.pescador.magicos");

        pescador.addAllowedTool(Material.FISHING_ROD);

        pescador.setParticleEffect(1, Particle.BUBBLE);
        pescador.setParticleEffect(10, Particle.SPLASH);
        pescador.setParticleEffect(20, Particle.DOLPHIN);

        availableClasses.put("pescador", pescador);

        // Classe Ferreiro
        ClassDefinition ferreiro = new ClassDefinition("Ferreiro");
        ferreiro.addLevelRequirement(1, 100);
        ferreiro.addLevelRequirement(2, 400);
        ferreiro.addLevelRequirement(3, 1000);
        ferreiro.addLevelRequirement(4, 1500);
        ferreiro.addLevelRequirement(5, 2000);
        ferreiro.addLevelRequirement(6, 3000);
        ferreiro.addLevelRequirement(7, 4000);
        ferreiro.addLevelRequirement(8, 5500);
        ferreiro.addLevelRequirement(9, 7000);
        ferreiro.addLevelRequirement(10, 9000);
        ferreiro.addLevelRequirement(11, 11000);
        ferreiro.addLevelRequirement(12, 13000);
        ferreiro.addLevelRequirement(13, 15000);
        ferreiro.addLevelRequirement(14, 18000);
        ferreiro.addLevelRequirement(15, 21000);
        ferreiro.addLevelRequirement(16, 24000);
        ferreiro.addLevelRequirement(17, 27000);
        ferreiro.addLevelRequirement(18, 30000);
        ferreiro.addLevelRequirement(19, 35000);
        ferreiro.addLevelRequirement(20, 40000);
        ferreiro.addLevelRequirement(21, 48000);
        ferreiro.addLevelRequirement(22, 58000);
        ferreiro.addLevelRequirement(23, 70000);
        ferreiro.addLevelRequirement(24, 85000);
        ferreiro.addLevelRequirement(25, 100000);
        ferreiro.addLevelRequirement(26, 120000);
        ferreiro.addLevelRequirement(27, 140000);
        ferreiro.addLevelRequirement(28, 165000);
        ferreiro.addLevelRequirement(29, 190000);
        ferreiro.addLevelRequirement(30, 215000);


        ferreiro.addLevelPermission(1, "classes.ferreiro.ferramentas.basicas");
        ferreiro.addLevelPermission(5, "classes.ferreiro.armas.basicas");
        ferreiro.addLevelPermission(10, "classes.ferreiro.ferramentas.avancadas");
        ferreiro.addLevelPermission(15, "classes.ferreiro.armas.avancadas");
        ferreiro.addLevelPermission(20, "classes.ferreiro.lendarias");

        ferreiro.addAllowedTool(Material.ANVIL);
        ferreiro.addAllowedTool(Material.SMITHING_TABLE);

        ferreiro.setParticleEffect(1, Particle.LAVA);
        ferreiro.setParticleEffect(10, Particle.FLAME);
        ferreiro.setParticleEffect(20, Particle.LARGE_SMOKE);

        availableClasses.put("ferreiro", ferreiro);

    }


    // Manipulação de eventos

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Carregar dados do jogador ou criar novos se não existirem
        if (!playerClasses.containsKey(playerId)) {
            loadPlayerData(playerId);
        }

        // Atualizar scoreboard
        updateScoreboard(player);

        // Mensagem de boas-vindas
        PlayerClassData data = playerClasses.get(playerId);
        if (data != null && data.getClassName() != null) {
            String className = data.getClassName();
            int level = data.getLevel();
            player.sendMessage(ChatColor.GREEN + "Bem-vindo de volta! Você é um " +
                    ChatColor.GOLD + className + ChatColor.GREEN +
                    " de nível " + ChatColor.YELLOW + level);
        } else {
            player.sendMessage(ChatColor.YELLOW + "Bem-vindo! Você ainda não escolheu uma classe.");
            player.sendMessage(ChatColor.YELLOW + "Use /classe escolher [nome] para selecionar sua classe.");
            player.sendMessage(ChatColor.AQUA + "Classes disponíveis: Minerador, Caçador, Pescador, Ferreiro");
        }
    }

    // Método auxiliar para verificar materiais de armadura
    private boolean isArmorMaterial(Material type, String material) {
        String name = type.name();
        return name.contains(material) &&
                (name.contains("HELMET") || name.contains("CHESTPLATE") ||
                        name.contains("LEGGINGS") || name.contains("BOOTS"));
    }


    /**
     * Abre o menu de seleção de classe para o jogador
     *
     * @param player Jogador que verá o menu
     * @param forced Se verdadeiro, força o jogador a escolher uma classe
     */
    public void openClassSelectionMenu(Player player, boolean forced) {
        // Agora chamamos o método no objeto classSelectionGUI, não como método estático
        ClassSelectionGUI.openClassSelectionMenu(player, forced);
    }

    @EventHandler
    public void onAnvilUse(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;

        // Verificar se o resultado tem Mending
        if (result.hasItemMeta() && result.getItemMeta().hasEnchant(Enchantment.MENDING)) {
            // Verificar se o jogador é um Ferreiro de nível adequado
            Player player = (Player) event.getView().getPlayer();
            UUID playerId = player.getUniqueId();
            PlayerClassData data = playerClasses.get(playerId);

            if (data == null || !data.getClassName().equalsIgnoreCase("ferreiro") || data.getLevel() < 20) {
                // Definir o resultado como nulo para impedir a aplicação do encantamento
                event.setResult(null);
                player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 20+ podem aplicar o encantamento Mending!");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageWithAxe(EntityDamageByEntityEvent event) {
        // Verificar se o dano foi causado por um jogador
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        UUID playerId = player.getUniqueId();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();
        int level = data.getLevel();

        // Verificar se o jogador está usando um machado
        ItemStack weapon = player.getInventory().getItemInMainHand();
        Material weaponType = weapon.getType();

        if (weaponType.name().contains("_AXE")) {
            // Permitir machados de madeira e pedra para todos
            if (weaponType == Material.WOODEN_AXE || weaponType == Material.STONE_AXE) {
                return;
            }

            if (className.equals("cacador")) {
                // Verificar nível do caçador para usar machados avançados
                boolean allowed = true;
                String requiredLevel = "";

                if (weaponType == Material.IRON_AXE && level < 5) {
                    allowed = false;
                    requiredLevel = "5";
                } else if (weaponType == Material.GOLDEN_AXE && level < 10) {
                    allowed = false;
                    requiredLevel = "10";
                } else if (weaponType == Material.DIAMOND_AXE && level < 15) {
                    allowed = false;
                    requiredLevel = "15";
                } else if (weaponType == Material.NETHERITE_AXE && level < 20) {
                    allowed = false;
                    requiredLevel = "20";
                }

                if (!allowed) {
                    // Cancelar o ataque
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você precisa ser um Caçador de nível " +
                            requiredLevel + " para usar este machado em combate!");

                    // Efeito visual e sonoro para feedback
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                    player.getWorld().spawnParticle(Particle.SMOKE,
                            player.getLocation().add(0, 1, 0),
                            5, 0.2, 0.2, 0.2, 0.05);
                }
            } else {
                // Outras classes não podem usar machados avançados em combate
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Apenas Caçadores podem usar machados avançados em combate!");

                // Efeito visual e sonoro para feedback
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                player.getWorld().spawnParticle(Particle.SMOKE,
                        player.getLocation().add(0, 1, 0),
                        5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorShiftClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClick() != ClickType.SHIFT_LEFT && event.getClick() != ClickType.SHIFT_RIGHT) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) return;

        int level = data.getLevel();

        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() != Material.AIR) {
            Material type = item.getType();

            // Verificar se é armadura de diamante ou netherite
            if (isArmorMaterial(type, "DIAMOND") && level < 15) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você precisa ser nível 15+ para equipar armaduras de diamante!");
            } else if (isArmorMaterial(type, "NETHERITE") && level < 25) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você precisa ser nível 25+ para equipar armaduras de netherite!");
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractSmithingTable(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material blockType = event.getClickedBlock().getType();
        if (blockType == Material.SMITHING_TABLE) {
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();

            PlayerClassData data = playerClasses.get(playerId);
            if (data == null || data.getClassName() == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe para usar a Mesa de Ferraria!");
                return;
            }

            String className = data.getClassName().toLowerCase();
            int level = data.getLevel();

            // Apenas ferreiros nível 10+ podem usar a Smithing Table
            if (!className.equals("ferreiro") || level < 10) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 10+ podem usar a Mesa de Ferraria!");
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onSmithingTableResult(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) return;

        Player player = (Player) event.getView().getPlayer();
        UUID playerId = player.getUniqueId();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) return;

        String className = data.getClassName().toLowerCase();
        int level = data.getLevel();

        // Verificar se é ferreiro nível 25+ para criar itens de netherite
        if (className.equals("ferreiro") && level < 25) {
            if (result.getType().name().contains("NETHERITE")) {
                event.setResult(null);
                player.sendMessage(ChatColor.RED + "Você precisa ser um Ferreiro de nível 25+ para criar itens de Netherite!");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Verificar se o dano foi causado por um jogador
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        UUID playerId = player.getUniqueId();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();
        int level = data.getLevel();

        // Verificar se o jogador está usando uma espada
        ItemStack weapon = player.getInventory().getItemInMainHand();
        Material weaponType = weapon.getType();

        if (weaponType.name().contains("SWORD")) {
            // Permitir espadas de madeira e pedra para todos
            if (weaponType == Material.WOODEN_SWORD || weaponType == Material.STONE_SWORD) {
                return;
            }

            if (className.equals("cacador")) {
                // Verificar nível do caçador para usar espadas avançadas
                boolean allowed = true;
                String requiredLevel = "";

                if (weaponType == Material.IRON_SWORD && level < 5) {
                    allowed = false;
                    requiredLevel = "5";
                } else if (weaponType == Material.GOLDEN_SWORD && level < 10) {
                    allowed = false;
                    requiredLevel = "10";
                } else if (weaponType == Material.DIAMOND_SWORD && level < 15) {
                    allowed = false;
                    requiredLevel = "15";
                } else if (weaponType == Material.NETHERITE_SWORD && level < 20) {
                    allowed = false;
                    requiredLevel = "20";
                }

                if (!allowed) {
                    // Cancelar o ataque
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você precisa ser um Caçador de nível " +
                            requiredLevel + " para usar esta espada eficientemente!");

                    // Efeito visual e sonoro para feedback
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                    player.spawnParticle(Particle.SMOKE, event.getEntity().getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                }
            } else {
                // Outras classes não podem usar espadas avançadas
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Apenas Caçadores podem usar espadas avançadas!");

                // Efeito visual e sonoro para feedback
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                player.spawnParticle(Particle.SMOKE, event.getEntity().getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    // Para verificar o desgaste das ferramentas com base na classe
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerClassData data = playerClasses.get(playerId);
        ItemStack item = event.getItem();

        // Verificar tipo de ferramenta
        Material type = item.getType();

        // Verificar classe do jogador
        String className = data != null && data.getClassName() != null ?
                data.getClassName().toLowerCase() : null;

        // Aplicar penalidade de durabilidade para classes incorretas
        if (type.name().contains("PICKAXE")) {
            // Picaretas são para Mineradores
            if (className == null || !className.equals("minerador")) {
                event.setDamage(event.getDamage() * 3); // 3x mais desgaste

                // Notificar com uma chance para evitar spam
                if (Math.random() < 0.2) {
                    player.sendMessage(ChatColor.RED + "Esta ferramenta está se desgastando rapidamente pois você não é um Minerador!");
                }
            }
        } else if (type == Material.FISHING_ROD) {
            // Varas de pesca são para Pescadores
            if (className == null || !className.equals("pescador")) {
                event.setDamage(event.getDamage() * 3);
            }
        } else if (type.name().contains("SWORD") || type.name().contains("BOW")) {
            // Armas são para Caçadores
            if (className == null || !className.equals("cacador")) {
                event.setDamage(event.getDamage() * 3);
            }
        }
    }

    // Métodos auxiliares
    private boolean estaNoCooldown(UUID playerId) {
        if (!ferreiroCooldowns.containsKey(playerId)) return false;
        return System.currentTimeMillis() < ferreiroCooldowns.get(playerId);
    }

    private void definirCooldown(UUID playerId, int segundos) {
        ferreiroCooldowns.put(playerId, System.currentTimeMillis() + (segundos * 1000L));
    }

    private void checkAllToolRestrictions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            PlayerClassData data = playerClasses.get(playerId);

            if (data == null || data.getClassName() == null) {
                continue;
            }

            String className = data.getClassName().toLowerCase();
            ClassDefinition classDef = availableClasses.get(className);
            int level = data.getLevel();

            // Verificar o item na mão principal
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() != Material.AIR) {
                Material material = mainHand.getType();

                // Permitir ferramentas básicas de madeira para todos
                if (material == Material.WOODEN_PICKAXE || material == Material.WOODEN_AXE) {
                    continue;
                }

                // Caso 1: Minerador com picareta avançada sem nível suficiente
                if (className.equals("minerador")) {
                    if ((material == Material.DIAMOND_PICKAXE && level < 15) ||
                            (material == Material.NETHERITE_PICKAXE && level < 25)) {

                        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 2, false, false, true));

                        if (Math.random() < 0.1) {
                            player.sendMessage(ChatColor.RED + "Esta picareta é muito avançada para seu nível atual!");
                        }
                    }
                }
                // Caso 2: Qualquer classe usando ferramenta restrita
                else if (isRestrictedTool(material) && !classDef.isAllowedTool(material)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 2, false, false, true));

                    if (Math.random() < 0.1) {
                    }
                }
            }
        }
    }

    // Adicione este método para verificar ferramentas avançadas para mineradores
    private void checkAdvancedToolsForMiners() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            PlayerClassData data = playerClasses.get(playerId);

            if (data == null || data.getClassName() == null) {
                continue;
            }

            String className = data.getClassName().toLowerCase();
            int level = data.getLevel();

            // Verificar apenas para mineradores
            if (className.equals("minerador")) {
                // Verificar o item na mão principal
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                Material toolType = mainHand.getType();

                // Verificar picaretas avançadas para níveis insuficientes
                if ((toolType == Material.DIAMOND_PICKAXE && level < 15) ||
                        (toolType == Material.NETHERITE_PICKAXE && level < 25)) {

                    // Aplicar efeito de fadiga de mineração constante
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 2, false, false, true));

                    // Enviar mensagem com uma chance baixa para não spammar o chat
                    if (Math.random() < 0.1) { // 10% de chance
                        player.sendMessage(ChatColor.RED + "Esta picareta é muito avançada para seu nível atual!");
                    }
                }
            }
        }
    }

    // Verificar quando um jogador usa uma ferramenta encantada com Fortune

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;

        Player player = (Player) event.getView().getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerClassData data = playerClasses.get(playerId);

        // Verificar se o resultado tem encantamentos
        if (result.hasItemMeta() && result.getItemMeta().hasEnchants()) {
            // Verificar se o jogador é ferreiro com nível adequado
            boolean isSmith = data != null && data.getClassName() != null &&
                    data.getClassName().equalsIgnoreCase("ferreiro");

            // Se não for ferreiro de nível 10+, bloquear QUALQUER encantamento
            if (!isSmith || (isSmith && data.getLevel() < 10)) {
                event.setResult(null);
                player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 10+ podem encantar itens!");
                return;
            }

            // Verificar cada tipo de encantamento específico
            for (Enchantment enchant : result.getItemMeta().getEnchants().keySet()) {
                String enchantName = enchant.getName().toLowerCase();
                int enchantLevel = result.getItemMeta().getEnchantLevel(enchant);

                // Verificar Efficiency
                if (enchantName.contains("efficiency") || enchantName.contains("dig_speed")) {
                    boolean isMiner = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("minerador");
                    int allowedLevel = 0;

                    if (isMiner) {
                        if (data.getLevel() >= 1) allowedLevel = 1;
                        if (data.getLevel() >= 5) allowedLevel = 2;
                        if (data.getLevel() >= 10) allowedLevel = 3;
                        if (data.getLevel() >= 15) allowedLevel = 4;
                        if (data.getLevel() >= 20) allowedLevel = 5;
                    } else if (isSmith) {
                        if (data.getLevel() >= 1) allowedLevel = 1;
                        if (data.getLevel() >= 5) allowedLevel = 2;
                        if (data.getLevel() >= 10) allowedLevel = 3;
                        if (data.getLevel() >= 15) allowedLevel = 4;
                        if (data.getLevel() >= 20) allowedLevel = 5;
                    }

                    if (enchantLevel > allowedLevel) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Efficiency " + enchantLevel + " requer " +
                                (isMiner ? "Minerador" : "Ferreiro") + " de nível mais alto!");
                        return;
                    }
                }

// Verificar Fortune
                if (enchantName.contains("fortune") || enchantName.contains("loot_bonus")) {
                    boolean isMiner = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("minerador");
                    // Não declaramos isSmith de novo, pois já existe no escopo
                    int allowedLevel = 0;

                    if (isMiner) {
                        if (data.getLevel() >= 10) allowedLevel = 1;
                        if (data.getLevel() >= 20) allowedLevel = 2;
                        if (data.getLevel() >= 30) allowedLevel = 3;
                    } else if (isSmith) { // Usamos a variável isSmith que já existe
                        if (data.getLevel() >= 12) allowedLevel = 1;
                        if (data.getLevel() >= 22) allowedLevel = 2;
                        if (data.getLevel() >= 30) allowedLevel = 3;
                    }

                    if (!isMiner && !isSmith) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Apenas Mineradores e Ferreiros podem aplicar Fortune!");
                        return;
                    } else if (enchantLevel > allowedLevel) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Você não tem nível suficiente para aplicar Fortune " + enchantLevel + "!");
                        return;
                    }
                }

                // Verificar Sharpness (Afiação)
                if (enchantName.contains("sharpness") || enchantName.contains("damage_all")) {
                    boolean isCacador = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("cacador");
                    boolean isFerreiro = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("ferreiro");
                    int allowedLevel = 0;

                    if (isCacador || isFerreiro) {
                        if (data.getLevel() >= 1) allowedLevel = 1;
                        if (data.getLevel() >= 5) allowedLevel = 2;
                        if (data.getLevel() >= 10) allowedLevel = 3;
                        if (data.getLevel() >= 15) allowedLevel = 4;
                        if (data.getLevel() >= 20) allowedLevel = 5;
                    }

                    if (enchantLevel > allowedLevel) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Afiação " + enchantLevel + " requer " +
                                (isCacador ? "Caçador" : "Ferreiro") + " de nível mais alto!");
                        return;
                    }
                }

                // Verificar Looting (Saque)
                if (enchantName.contains("looting") || enchantName.contains("loot_bonus_mobs")) {
                    boolean isCacador = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("cacador");
                    int allowedLevel = 0;

                    if (isCacador) {
                        if (data.getLevel() >= 10) allowedLevel = 1;
                        if (data.getLevel() >= 20) allowedLevel = 2;
                        if (data.getLevel() >= 30) allowedLevel = 3;
                    }

                    if (enchantLevel > allowedLevel) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Você não tem nível suficiente para aplicar Saque " + enchantLevel + "!");
                        return;
                    }
                }

                // Verificar Silk Touch (Toque Suave)
                if (enchantName.contains("silk_touch")) {
                    boolean isMiner = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("minerador");
                    boolean isFerreiro = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("ferreiro");

                    if ((!isMiner && !isFerreiro) || (data.getLevel() < 20)) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Apenas Mineradores ou Ferreiros de nível 20+ podem aplicar Toque Suave!");
                        return;
                    }
                }

                // Verificar Protection (Proteção)
                if (enchantName.contains("protection") || enchantName.contains("protection_environmental")) {
                    boolean isFerreiro = data != null && data.getClassName() != null &&
                            data.getClassName().equalsIgnoreCase("ferreiro");
                    int allowedLevel = 0;

                    if (isFerreiro) {
                        if (data.getLevel() >= 1) allowedLevel = 1;
                        if (data.getLevel() >= 5) allowedLevel = 2;
                        if (data.getLevel() >= 10) allowedLevel = 3;
                        if (data.getLevel() >= 15) allowedLevel = 4;
                        if (data.getLevel() >= 20) allowedLevel = 5;
                    }

                    if (!isFerreiro || enchantLevel > allowedLevel) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Apenas Ferreiros podem aplicar Proteção " + enchantLevel + "!");
                        return;
                    }
                }

                // Verificar Mending
                if (enchantName.contains("mending")) {
                    if (!isSmith || (isSmith && data.getLevel() < 20)) {
                        event.setResult(null);
                        player.sendMessage(ChatColor.RED + "Apenas ferreiros de nível 20+ podem aplicar Mending!");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBowShoot(EntityShootBowEvent event) {
        // Verificar se quem atirou é um jogador
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();
        int level = data.getLevel();

        // Obter o tipo de arma
        Material bowType = event.getBow().getType();

        // Verificar restrições de classe para arcos e bestas
        if (bowType == Material.BOW || bowType == Material.CROSSBOW) {
            if (className.equals("cacador")) {
                // Restrições baseadas em nível para caçadores
                boolean allowed = true;
                String requiredLevel = "";

                if (bowType == Material.BOW && level < 5) {
                    allowed = false;
                    requiredLevel = "5";
                } else if (bowType == Material.CROSSBOW && level < 15) {
                    allowed = false;
                    requiredLevel = "15";
                }

                if (!allowed) {
                    // Cancelar o tiro
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você precisa ser um Caçador de nível " +
                            requiredLevel + " para usar este arco eficientemente!");

                    // Efeito visual e sonoro para feedback
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                    player.getWorld().spawnParticle(Particle.SMOKE,
                            player.getLocation().add(0, 1, 0),
                            5, 0.2, 0.2, 0.2, 0.05);

                    // Devolver a flecha ao inventário
                    if (event.getConsumable() != null) {
                        player.getInventory().addItem(event.getConsumable().clone());
                    }
                }
            } else {
                // Outras classes não podem usar arcos
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Apenas Caçadores podem usar arcos e bestas!");

                // Efeito visual e sonoro para feedback
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                player.getWorld().spawnParticle(Particle.SMOKE,
                        player.getLocation().add(0, 1, 0),
                        5, 0.2, 0.2, 0.2, 0.05);

                // Devolver a flecha ao inventário
                if (event.getConsumable() != null) {
                    player.getInventory().addItem(event.getConsumable().clone());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTitle().contains("Anvil") || event.getView().getTitle().contains("Bigorna"))) {

            if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    UUID playerId = player.getUniqueId();

                    PlayerClassData data = playerClasses.get(playerId);
                    if (data == null || data.getClassName() == null) return;

                    int level = data.getLevel();

                    ItemStack item = event.getCursor();
                    if (item != null && item.getType() != Material.AIR) {
                        Material type = item.getType();

                        // Verificar se é armadura de diamante ou netherite
                        if (isArmorMaterial(type, "DIAMOND") && level < 15) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "Você precisa ser nível 15+ para equipar armaduras de diamante!");
                            return;
                        } else if (isArmorMaterial(type, "NETHERITE") && level < 25) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "Você precisa ser nível 25+ para equipar armaduras de netherite!");
                            return;
                        }
                    }
                }
            }
        }

        // Verificar se é o slot de resultado
        if (event.getRawSlot() == 2 && event.getCurrentItem() != null) {
            ItemStack result = event.getCurrentItem();

            if (result.hasItemMeta() && result.getItemMeta().hasEnchants()) {
                Player player = (Player) event.getWhoClicked();
                UUID playerId = player.getUniqueId();
                PlayerClassData data = playerClasses.get(playerId);

                // Esta verificação de fallback é mais simples e genérica:
                boolean isSmith = data != null && data.getClassName() != null &&
                        data.getClassName().equalsIgnoreCase("ferreiro");

                // Se não for ferreiro, bloquear qualquer encantamento
                if (!isSmith || (isSmith && data.getLevel() < 10)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 10+ podem encantar itens!");
                    return;
                }

                // Verificações específicas para encantamentos
                for (Enchantment enchant : result.getItemMeta().getEnchants().keySet()) {
                    String enchName = enchant.getName().toLowerCase();

                    // Mending
                    if (enchName.contains("mending") && (!isSmith || data.getLevel() < 20)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 20+ podem aplicar Mending!");
                        return;

                    }

// Efficiency - mesmo nível para mineradores e ferreiros
                    if ((enchName.contains("efficiency") || enchName.contains("dig_speed"))) {
                        boolean isMiner = data.getClassName().equalsIgnoreCase("minerador");
                        boolean canUseEnchant = false;
                        int enchLevel = result.getItemMeta().getEnchantLevel(enchant);

                        // Mesmo requisito para ambos
                        if (isMiner || isSmith) {
                            canUseEnchant = (data.getLevel() >= 5 && enchLevel <= 1) ||
                                    (data.getLevel() >= 10 && enchLevel <= 2) ||
                                    (data.getLevel() >= 15 && enchLevel <= 3) ||
                                    (data.getLevel() >= 20 && enchLevel <= 4) ||
                                    (data.getLevel() >= 25 && enchLevel <= 5);
                        }

                        if (!canUseEnchant) {
                            event.setCancelled(true);
                            String classe = isMiner ? "Minerador" : "Ferreiro";
                            player.sendMessage(ChatColor.RED + "Efficiency " + enchLevel + " requer " +
                                    classe + " de nível " + (enchLevel * 5) + "+");
                            return;
                        }
                    }

// Fortune - mesmo nível para mineradores e ferreiros
                    if (enchName.contains("fortune") || enchName.contains("loot_bonus")) {
                        boolean isMinerClass = data.getClassName().equalsIgnoreCase("minerador");

                        if (!isMinerClass && !isSmith) {
                            // Nem Minerador nem ferreiro - não pode usar Fortune
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "Apenas Mineradores e Ferreiros podem aplicar Fortune!");
                            return;
                        } else {
                            int enchLevel = result.getItemMeta().getEnchantLevel(enchant);
                            boolean hasLevel = false;

                            // Mesmo requisito para ambos
                            hasLevel = (data.getLevel() >= 10 && enchLevel <= 1) ||
                                    (data.getLevel() >= 20 && enchLevel <= 2) ||
                                    (data.getLevel() >= 30 && enchLevel <= 3);

                            if (!hasLevel) {
                                event.setCancelled(true);
                                player.sendMessage(ChatColor.RED + "Fortune " + enchLevel + " requer nível " +
                                        (enchLevel * 10) + "+");
                                return;

                            }

                        }
                    }
                }
            }
        }
    }

    // Métodos auxiliares para obter os encantamentos (independente da versão)
    private Enchantment getEfficiencyEnchantment() {
        try {
            // Tentar obter por nome
            return Enchantment.getByName("EFFICIENCY");
        } catch (Exception e) {
            try {
                // Tentar obter por key (versões mais recentes)
                return Enchantment.getByKey(NamespacedKey.minecraft("efficiency"));
            } catch (Exception ex) {
                // Fallback para algum método específico da versão
                for (Enchantment ench : Enchantment.values()) {
                    if (ench.getName().equalsIgnoreCase("efficiency") ||
                            ench.getName().toLowerCase().contains("dig_speed")) {
                        return ench;
                    }
                }
                return null;
            }
        }
    }

    private Enchantment getMendingEnchantment() {
        try {
            return Enchantment.getByName("MENDING");
        } catch (Exception e) {
            try {
                return Enchantment.getByKey(NamespacedKey.minecraft("mending"));
            } catch (Exception ex) {
                for (Enchantment ench : Enchantment.values()) {
                    if (ench.getName().equalsIgnoreCase("mending")) {
                        return ench;
                    }
                }
                return null;
            }
        }
    }

    private Enchantment getFortuneEnchantment() {
        try {
            return Enchantment.getByName("FORTUNE");
        } catch (Exception e) {
            try {
                return Enchantment.getByKey(NamespacedKey.minecraft("fortune"));
            } catch (Exception ex) {
                for (Enchantment ench : Enchantment.values()) {
                    if (ench.getName().equalsIgnoreCase("fortune") ||
                            ench.getName().toLowerCase().contains("loot_bonus_blocks")) {
                        return ench;
                    }
                }
                return null;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        UUID playerId = player.getUniqueId();
        PlayerClassData data = playerClasses.get(playerId);

        // Verificar se tem dados válidos
        if (data == null || data.getClassName() == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe para encantar itens!");
            return;
        }

        // Verificar se é ferreiro de nível adequado
        boolean isSmith = data.getClassName().equalsIgnoreCase("ferreiro");
        if (!isSmith || (isSmith && data.getLevel() < 10)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 10+ podem encantar itens!");
            return;
        }

        // Verificar os encantamentos gerados
        Map<Enchantment, Integer> enchs = event.getEnchantsToAdd();
        for (Map.Entry<Enchantment, Integer> entry : enchs.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            String enchName = enchant.getName().toLowerCase();

            // Verificar Mending - apenas nível 20+
            if (enchName.contains("mending") && data.getLevel() < 20) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 20+ podem obter remendo!");
                return;
            }


            // Verificar Sharpness (Afiação)
            if (enchName.contains("sharpness") || enchName.contains("damage_all")) {
                int allowedLevel = 0;
                if (data.getLevel() >= 1) allowedLevel = 1;
                if (data.getLevel() >= 5) allowedLevel = 2;
                if (data.getLevel() >= 10) allowedLevel = 3;
                if (data.getLevel() >= 15) allowedLevel = 4;
                if (data.getLevel() >= 20) allowedLevel = 5;

                if (level > allowedLevel) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Seu nível não é alto o suficiente para este nível de Afiação!");
                    return;
                }
            }

            // Verificar Looting (Saque)
            if (enchName.contains("looting") || enchName.contains("loot_bonus_mobs")) {
                int allowedLevel = 0;
                if (data.getLevel() >= 10) allowedLevel = 1;
                if (data.getLevel() >= 20) allowedLevel = 2;
                if (data.getLevel() >= 30) allowedLevel = 3;

                if (level > allowedLevel) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Seu nível não é alto o suficiente para este nível de Saque!");
                    return;
                }
            }

            // Verificar Silk Touch (Toque Suave)
            if (enchName.contains("silk_touch")) {
                if (data.getLevel() < 20) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nível 20+ podem obter Toque Suave!");
                    return;
                }
            }

            // Verificar Infinidade
            if (enchName.contains("infinity")) {
                if (data.getLevel() < 15) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Apenas Ferreiros de nivel 15+ podem obter Infinidade!");
                    return;
                }
            }

            // Verificar Protection (Proteção)
            if (enchName.contains("protection") || enchName.contains("protection_environmental")) {
                int allowedLevel = 0;
                if (data.getLevel() >= 1) allowedLevel = 1;
                if (data.getLevel() >= 5) allowedLevel = 2;
                if (data.getLevel() >= 10) allowedLevel = 3;
                if (data.getLevel() >= 15) allowedLevel = 4;
                if (data.getLevel() >= 20) allowedLevel = 5;

                if (level > allowedLevel) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Seu nível não é alto o suficiente para este nível de Proteção!");
                    return;
                }
            }

            // Verificar Fortune - níveis específicos
            if ((enchName.contains("fortune") || enchName.contains("loot_bonus")) &&
                    (level > 1 && data.getLevel() < 22 ||
                            level > 2 && data.getLevel() < 30 ||
                            level > 0 && data.getLevel() < 12)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Seu nível não é alto o suficiente para este nível de Fortuna!");
                return;
            }

            // Verificar Efficiency - níveis específicos
            if ((enchName.contains("efficiency") || enchName.contains("dig_speed"))) {
                int maxLevel = 0;
                if (data.getLevel() >= 1) maxLevel = 1;
                if (data.getLevel() >= 5) maxLevel = 2;
                if (data.getLevel() >= 10) maxLevel = 3;
                if (data.getLevel() >= 15) maxLevel = 4;
                if (data.getLevel() >= 20) maxLevel = 5;

                if (level > maxLevel) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Seu nível não é alto o suficiente para este nível de Eficiencia!");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Material blockType = event.getBlock().getType();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();
        ClassDefinition classDef = availableClasses.get(className);

        // Verificar se é um bloco de mineração
        if (isMiningBlock(blockType)) {
            // Verificar qual ferramenta o jogador está usando
            ItemStack tool = player.getInventory().getItemInMainHand();

            if (className.equals("minerador")) {
                // Verificar se tem o nível necessário para este minério
                if (!hasRequiredLevelForOre(data, blockType)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você não tem nível suficiente para minerar este recurso!");
                    return;

                }

                Material toolType = tool.getType();

                // Verificar nível necessário para picaretas avançadas
                if ((toolType == Material.DIAMOND_PICKAXE && data.getLevel() < 15) ||
                        (toolType == Material.NETHERITE_PICKAXE && data.getLevel() < 25)) {

                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você precisa ser um Minerador de nível " +
                            (toolType == Material.DIAMOND_PICKAXE ? "15" : "25") +
                            " para usar esta picareta!");
                    return;
                }

                // Verificar se está usando a ferramenta certa
                if (!classDef.isAllowedTool(tool.getType())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você precisa de uma ferramenta de minerador adequada!");
                    return;
                }

                // Adicionar XP com multiplicador e verificar missões
                int baseXp = getXpForMining(blockType);
                int xpGained = baseXp;

                // Aplicar multiplicador de XP se existir
                if (xpMultiplierSystem.hasActiveMultiplier(playerId)) {
                    xpGained = xpMultiplierSystem.applyMultiplier(playerId, baseXp);
                    int bonus = xpGained - baseXp;
                    if (bonus > 0) {
                        player.sendMessage(ChatColor.AQUA + "+" + bonus + " XP bônus do multiplicador!");
                    }
                }

                // Adicionar XP apenas UMA vez, depois do multiplicador ter sido aplicado
                data.addXp(xpGained);
                player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Minerador");

// Verificar se o jogador encontrou uma gema rara
                if (isMiningBlock(blockType)) {
                    // Formatar localização do bloco para prevenção de exploit
                    String blockLoc = event.getBlock().getWorld().getName() + "," +
                            event.getBlock().getX() + "," +
                            event.getBlock().getY() + "," +
                            event.getBlock().getZ();

                    // Verificar se é um bloco natural
                    boolean isNaturalBlock = !event.getBlock().hasMetadata("player_placed");

                    // Tentar encontrar uma gema
                    ItemStack gemFound = gemSystem.tryFindGem(player, blockType, blockLoc, data.getLevel(), isNaturalBlock);

                    if (gemFound != null) {
                        // Dar a gema ao jogador
                        player.getInventory().addItem(gemFound);

                        // Efeitos visuais e sonoros para tornar o momento especial
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                        player.spawnParticle(Particle.LAVA, event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                                20, 0.3, 0.3, 0.3, 0.05);

                        // Anunciar para todos os jogadores próximos
                        RareGemSystem.GemType gemType = gemSystem.getGemType(gemFound);
                        if (gemType != null) {
                            String gemName = gemType.getDisplayName();
                            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                                if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 30) {
                                    nearbyPlayer.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.WHITE + player.getName() +
                                            ChatColor.YELLOW + " encontrou uma " + gemName + ChatColor.YELLOW + "!");
                                }
                            }
                        }
                    }
                }

                // Verificar level up
                checkLevelUp(player, data);

                // Atualizar scoreboard
                updateScoreboard(player);
            } else {
                // Permitir uso de picareta de madeira para qualquer classe, mas com restrições
                if (tool.getType() == Material.WOODEN_PICKAXE) {
                    // Dar apenas 1 XP independente do minério (eficiência reduzida)
                    int baseXp = 1;
                    int xpGained = baseXp;
                    data.addXp(xpGained);
                    player.sendMessage(ChatColor.YELLOW + "+" + xpGained + " XP de " + data.getClassName() +
                            " (reduzido por não ser sua especialidade)");

                    // Chance de 50% de não dropar o item
                    if (Math.random() < 0.5) {
                        event.setDropItems(false);
                        player.sendMessage(ChatColor.RED + "Você não conseguiu extrair corretamente o minério.");
                    }

                    // Continue permitindo a quebra do bloco
                    return;
                }

                // Para outras ferramentas, manter o bloqueio
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Como você não é um Minerador, não conseguiu extrair este minério corretamente.");
                event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            }
        }
    }

    // Para lidar com encantamentos (como alternativa ao EnchantItemEvent)
    @EventHandler
    public void onPlayerInteractEnchantingTable(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.ENCHANTING_TABLE) {

            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            PlayerClassData data = playerClasses.get(playerId);

            // Verificar se é um Ferreiro de nível 10+
            if (data == null || !data.getClassName().equalsIgnoreCase("ferreiro") || data.getLevel() < 10) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Apenas Ferreiros de @EventHandler(priority = EventPriority.LOWEST)\n" +
                        "public void onEntityDamageWithAxe(EntityDamageByEntityEvent event) {\n" +
                        "    // Verificar se o dano foi causado por um jogador\n" +
                        "    if (!(event.getDamager() instanceof Player)) {\n" +
                        "        return;\n" +
                        "    }\n" +
                        "    \n" +
                        "    Player player = (Player) event.getDamager();\n" +
                        "    UUID playerId = player.getUniqueId();\n" +
                        "    \n" +
                        "    PlayerClassData data = playerClasses.get(playerId);\n" +
                        "    if (data == null || data.getClassName() == null) {\n" +
                        "        return;\n" +
                        "    }\n" +
                        "    \n" +
                        "    String className = data.getClassName().toLowerCase();\n" +
                        "    int level = data.getLevel();\n" +
                        "    \n" +
                        "    // Verificar se o jogador está usando um machado\n" +
                        "    ItemStack weapon = player.getInventory().getItemInMainHand();\n" +
                        "    Material weaponType = weapon.getType();\n" +
                        "    \n" +
                        "    if (weaponType.name().contains(\"_AXE\")) {\n" +
                        "        // Permitir machados de madeira e pedra para todos\n" +
                        "        if (weaponType == Material.WOODEN_AXE || weaponType == Material.STONE_AXE) {\n" +
                        "            return;\n" +
                        "        }\n" +
                        "        \n" +
                        "        if (className.equals(\"cacador\")) {\n" +
                        "            // Verificar nível do caçador para usar machados avançados\n" +
                        "            boolean allowed = true;\n" +
                        "            String requiredLevel = \"\";\n" +
                        "            \n" +
                        "            if (weaponType == Material.IRON_AXE && level < 5) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"5\";\n" +
                        "            } else if (weaponType == Material.GOLDEN_AXE && level < 10) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"10\";\n" +
                        "            } else if (weaponType == Material.DIAMOND_AXE && level < 15) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"15\";\n" +
                        "            } else if (weaponType == Material.NETHERITE_AXE && level < 20) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"20\";\n" +
                        "            }\n" +
                        "            \n" +
                        "            if (!allowed) {\n" +
                        "                // Cancelar o ataque\n" +
                        "                event.setCancelled(true);\n" +
                        "                player.sendMessage(ChatColor.RED + \"Você precisa ser um Caçador de nível \" + \n" +
                        "                                  requiredLevel + \" para usar este machado em combate!\");\n" +
                        "                \n" +
                        "                // Efeito visual e sonoro para feedback\n" +
                        "                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);\n" +
                        "                player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, \n" +
                        "                                              player.getLocation().add(0, 1, 0), \n" +
                        "                                              5, 0.2, 0.2, 0.2, 0.05);\n" +
                        "            }\n" +
                        "        } else {\n" +
                        "            // Outras classes não podem usar machados avançados em combate\n" +
                        "            event.setCancelled(true);\n" +
                        "            player.sendMessage(ChatColor.RED + \"Apenas Caçadores podem usar machados avançados em combate!\");\n" +
                        "            \n" +
                        "            // Efeito visual e sonoro para feedback\n" +
                        "            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);\n" +
                        "            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, \n" +
                        "                                          player.getLocation().add(0, 1, 0), \n" +
                        "                                          5, 0.2, 0.2, 0.2, 0.05);\n" +
                        "        }\n" +
                        "    }\n" +
                        "}@EventHandler(priority = EventPriority.LOWEST)\n" +
                        "public void onEntityDamageWithAxe(EntityDamageByEntityEvent event) {\n" +
                        "    // Verificar se o dano foi causado por um jogador\n" +
                        "    if (!(event.getDamager() instanceof Player)) {\n" +
                        "        return;\n" +
                        "    }\n" +
                        "    \n" +
                        "    Player player = (Player) event.getDamager();\n" +
                        "    UUID playerId = player.getUniqueId();\n" +
                        "    \n" +
                        "    PlayerClassData data = playerClasses.get(playerId);\n" +
                        "    if (data == null || data.getClassName() == null) {\n" +
                        "        return;\n" +
                        "    }\n" +
                        "    \n" +
                        "    String className = data.getClassName().toLowerCase();\n" +
                        "    int level = data.getLevel();\n" +
                        "    \n" +
                        "    // Verificar se o jogador está usando um machado\n" +
                        "    ItemStack weapon = player.getInventory().getItemInMainHand();\n" +
                        "    Material weaponType = weapon.getType();\n" +
                        "    \n" +
                        "    if (weaponType.name().contains(\"_AXE\")) {\n" +
                        "        // Permitir machados de madeira e pedra para todos\n" +
                        "        if (weaponType == Material.WOODEN_AXE || weaponType == Material.STONE_AXE) {\n" +
                        "            return;\n" +
                        "        }\n" +
                        "        \n" +
                        "        if (className.equals(\"cacador\")) {\n" +
                        "            // Verificar nível do caçador para usar machados avançados\n" +
                        "            boolean allowed = true;\n" +
                        "            String requiredLevel = \"\";\n" +
                        "            \n" +
                        "            if (weaponType == Material.IRON_AXE && level < 5) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"5\";\n" +
                        "            } else if (weaponType == Material.GOLDEN_AXE && level < 10) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"10\";\n" +
                        "            } else if (weaponType == Material.DIAMOND_AXE && level < 15) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"15\";\n" +
                        "            } else if (weaponType == Material.NETHERITE_AXE && level < 20) {\n" +
                        "                allowed = false;\n" +
                        "                requiredLevel = \"20\";\n" +
                        "            }\n" +
                        "            \n" +
                        "            if (!allowed) {\n" +
                        "                // Cancelar o ataque\n" +
                        "                event.setCancelled(true);\n" +
                        "                player.sendMessage(ChatColor.RED + \"Você precisa ser um Caçador de nível \" + \n" +
                        "                                  requiredLevel + \" para usar este machado em combate!\");\n" +
                        "                \n" +
                        "                // Efeito visual e sonoro para feedback\n" +
                        "                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);\n" +
                        "                player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, \n" +
                        "                                              player.getLocation().add(0, 1, 0), \n" +
                        "                                              5, 0.2, 0.2, 0.2, 0.05);\n" +
                        "            }\n" +
                        "        } else {\n" +
                        "            // Outras classes não podem usar machados avançados em combate\n" +
                        "            event.setCancelled(true);\n" +
                        "            player.sendMessage(ChatColor.RED + \"Apenas Caçadores podem usar machados avançados em combate!\");\n" +
                        "            \n" +
                        "            // Efeito visual e sonoro para feedback\n" +
                        "            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);\n" +
                        "            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, \n" +
                        "                                          player.getLocation().add(0, 1, 0), \n" +
                        "                                          5, 0.2, 0.2, 0.2, 0.05);\n" +
                        "        }\n" +
                        "    }\n" +
                        "} 10+ podem usar Mesas de Encantamento!");
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }

        Player player = event.getEntity().getKiller();
        UUID playerId = player.getUniqueId();
        EntityType entityType = event.getEntityType();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();
        ClassDefinition classDef = availableClasses.get(className);

        if (className.equals("cacador") && isHuntableEntity(entityType)) {
            // Verificar permissão de nível para este tipo de mob
            if (!hasRequiredLevelForEntity(data, entityType)) {
                player.sendMessage(ChatColor.RED + "Você matou esta criatura, mas não tem nível suficiente para ganhar XP dele!");
                return;
            }

            // Adicionar XP com multiplicador
            int baseXp = getXpForKilling(entityType);
            int xpGained = baseXp;

            // Aplicar multiplicador de XP se existir
            if (xpMultiplierSystem.hasActiveMultiplier(playerId)) {
                xpGained = xpMultiplierSystem.applyMultiplier(playerId, baseXp);
                int bonus = xpGained - baseXp;
                if (bonus > 0) {
                    player.sendMessage(ChatColor.AQUA + "+" + bonus + " XP bônus do multiplicador!");
                }
            }

            data.addXp(xpGained);
            player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Caçador");

            // Chance de drop especial para caçadores
            if (data.getLevel() >= 5 && Math.random() < 0.2) {  // 20% de chance em nível 5+
                ItemStack specialDrop = getSpecialHunterDrop(entityType, data.getLevel());
                if (specialDrop != null) {
                    event.getDrops().add(specialDrop);
                    player.sendMessage(ChatColor.GOLD + "Sua habilidade de caçador lhe concedeu um item especial!");
                }
            }

            // Verificar level up
            checkLevelUp(player, data);

            // Atualizar scoreboard
            updateScoreboard(player);

            // Lógica similar para o Guerreiro
            // ...

        } else if (isHostileEntity(entityType) && !className.equals("cacador")) {
            // Remover completamente o ganho de XP para não-caçadores
            player.sendMessage(ChatColor.RED + "Apenas Caçadores ganham XP por matar criaturas.");

            // Opcionalmente, adicionar uma mensagem explicativa
            if (Math.random() < 0.3) { // 30% de chance para reduzir spam
                player.sendMessage(ChatColor.GRAY + "Apenas caçadores conseguem usar espadas com maestria");
            }
        }
    }


    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();

        // Se não for pescador, cancelar o evento de pesca completamente
        if (!className.equals("pescador")) {
            event.setCancelled(true);

            // Verificar se não mandamos mensagens demais (para evitar spam)
            if (Math.random() < 0.3) { // 30% de chance de mostrar mensagem
                player.sendMessage(ChatColor.RED + "Você não possui conhecimento para usar uma vara de pesca corretamente!");
            }

            // Efeito de falha (opcional)
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5f, 0.5f);
            return;
        }

        // Resto da lógica para pescadores...
        ClassDefinition classDef = availableClasses.get(className);

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Verificar se está usando uma vara de pesca
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!classDef.isAllowedTool(tool.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você precisa de uma vara de pesca adequada!");
                return;
            }

            // Remover o item original pescado (peixe vanilla)
            Entity caught = event.getCaught();
            if (caught != null) {
                caught.remove();
            }

            // Aplicar velocidade de pesca baseada no nível
            int level = data.getLevel();
            applyFishingSpeedBonus(player, level);

            // Gerar um peixe customizado baseado no nível e se é pesca na lava

            ItemStack customFish = customFishSystem.getRandomFish(level);
            {
            }

            // Dropar o peixe no mundo
            player.getWorld().dropItemNaturally(player.getLocation(), customFish);

            // Obter XP do peixe
            int fishXp = customFishSystem.getFishXp(customFish);

            // Aplicar multiplicador de XP (se existir)
            int finalXp = fishXp;
            if (xpMultiplierSystem.hasActiveMultiplier(playerId)) {
                finalXp = xpMultiplierSystem.applyMultiplier(playerId, fishXp);
                int bonus = finalXp - fishXp;
                if (bonus > 0) {
                    player.sendMessage(ChatColor.AQUA + "+" + bonus + " XP bônus do multiplicador!");
                }
            }

            // Adicionar XP
            data.addXp(finalXp);

            // Determinar a raridade para efeitos visuais/sonoros
            CustomFishSystem.FishType fishType = customFishSystem.getFishType(customFish);
            String rarityMessage = "";

            if (fishType != null) {
                // Efeitos baseados na raridade
                if (fishType.ordinal() <= 1) { // Comum
                    rarityMessage = "Comum";
                    player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);
                } else if (fishType.ordinal() <= 3) { // Incomum
                    rarityMessage = "Incomum";
                    player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
                    player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                } else if (fishType.ordinal() <= 5) { // Raro
                    rarityMessage = "Raro";
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 20, 0.7, 0.7, 0.7, 0.2);
                } else { // Muito raro
                    rarityMessage = "Muito Raro";
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
                    player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 30, 1.0, 1.0, 1.0, 0.3);
                    player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 50, 1.0, 1.0, 1.0, 0.5);

                    // Anunciar para todos os jogadores próximos
                    String fishName = fishType.getDisplayName();
                    for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                        if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 50) {
                            nearbyPlayer.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.WHITE + player.getName() +
                                    ChatColor.YELLOW + " pescou um " + fishName + ChatColor.YELLOW + "!");
                        }
                    }
                }
            }

            // Mensagem de XP
            player.sendMessage(ChatColor.GREEN + "+" + finalXp + " XP de Pescador (" + fishType.getDisplayName() + ChatColor.GREEN + " - " + rarityMessage + ")");

            // FUNCIONALIDADE EXISTENTE: Chance de pescar um multiplicador de XP
            ItemStack multiplierItem = xpMultiplierSystem.getRandomMultiplierForLevel(data.getLevel());
            if (multiplierItem != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), multiplierItem);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "✨ Você pescou um item de multiplicador de XP raro! ✨");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            }

            // Verificar level up
            checkLevelUp(player, data);

            // Atualizar scoreboard
            updateScoreboard(player);
        }
    }

    private boolean isLavaFishing(PlayerFishEvent event) {
        return false;
    }

    private void applyFishingSpeedBonus(Player player, int level) {
    }

    private ItemStack getRandomLavaFish(int level) {
        return null;
    }
    private FurnaceXpListener furnaceXpListener;
    

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

            XpMultiplierSystem.Rarity rarity = null;

// Identificar a raridade com base no nome ou material
            if (displayName.contains("Incomum")) {
                rarity = XpMultiplierSystem.Rarity.UNCOMMON;
            } else if (displayName.contains("Raro")) {
                rarity = XpMultiplierSystem.Rarity.RARE;
            } else if (displayName.contains("Lendário")) {
                rarity = XpMultiplierSystem.Rarity.LEGENDARY;
            } else {
                return; // Não é um multiplicador válido
            }

            // Verificar se o jogador já tem um multiplicador ativo
            if (xpMultiplierSystem.hasActiveMultiplier(player.getUniqueId())) {
                // Não permitir ativar enquanto outro está ativo
                player.sendMessage(ChatColor.RED + "Você já possui um multiplicador de XP ativo!");
                return; // IMPORTANTE: retornamos sem consumir o item
            }

            // Ativar o multiplicador
            boolean activated = xpMultiplierSystem.activateMultiplier(player, rarity);

            if (activated) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInMainHand(item);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Verificar se a entidade é um villager
        if (event.getRightClicked() instanceof Villager) {
            Villager villager = (Villager) event.getRightClicked();
            Player player = event.getPlayer();

            String profession = villager.getProfession().name();

            // Bloquear profissões específicas (armeiros, ferramenteiros e bibliotecários)
            if (profession.equals("ARMORER") ||
                    profession.equals("WEAPONSMITH") ||
                    profession.equals("TOOLSMITH") ||
                    profession.equals("LIBRARIAN")) {

                // Cancelar a interação
                event.setCancelled(true);

                // Mostrar mensagem customizada para cada tipo
                if (profession.equals("ARMORER")) {
                    player.sendMessage(ChatColor.RED + "O Armeiro se recusa a negociar com você!");
                } else if (profession.equals("WEAPONSMITH")) {
                    player.sendMessage(ChatColor.RED + "O Armeiro de Armas se recusa a negociar com você!");
                } else if (profession.equals("TOOLSMITH")) {
                    player.sendMessage(ChatColor.RED + "O Ferramenteiro se recusa a negociar com você!");
                } else if (profession.equals("LIBRARIAN")) {
                    player.sendMessage(ChatColor.RED + "O Bibliotecário se recusa a negociar com você!");
                }

                // Efeito visual e sonoro
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.SMOKE,
                        villager.getLocation().add(0, 1.5, 0),
                        5, 0.3, 0.3, 0.3, 0.1);
                return;
            }

            // Restrição para Fletcher - apenas caçadores nível 8+
            if (profession.equals("FLETCHER")) {
                // Verificar se o jogador tem dados de classe válidos
                PlayerClassData playerData = playerClasses.get(player.getUniqueId());
                if (playerData == null || playerData.getClassName() == null) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe para negociar com Fletchers!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // Verificar se é caçador de nível 8+
                String playerClass = playerData.getClassName().toLowerCase();
                int playerLevel = playerData.getLevel();

                if (!playerClass.equals("cacador") || playerLevel < 8) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Apenas Caçadores de nível 8+ podem negociar com Fletchers!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
            }
        }
    }

    // Adicionar este evento para controlar as compras específicas do Cleric
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTradeSelect(InventoryClickEvent event) {
        // Verificar se é inventário de mercante
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            // Verificar se é slot de resultado (2)
            if (event.getRawSlot() == 2 && event.getCurrentItem() != null) {
                Player player = (Player) event.getWhoClicked();
                UUID playerId = player.getUniqueId();
                ItemStack result = event.getCurrentItem();

                PlayerClassData data = playerClasses.get(playerId);
                if (data == null || data.getClassName() == null) return;

                String className = data.getClassName().toLowerCase();
                int level = data.getLevel();

                // Verificar o tipo do item
                Material itemType = result.getType();

                // Restrições para itens do Cleric
                if (itemType == Material.REDSTONE) {
                    if (!className.equals("minerador") || level < 7) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Apenas Mineradores de nível 7+ podem comprar Redstone!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                } else if (itemType == Material.LAPIS_LAZULI) {
                    if (!className.equals("minerador") || level < 12) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Apenas Mineradores de nível 12+ podem comprar Lapis Lazuli!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                } else if (itemType == Material.ENDER_PEARL) {
                    if (!className.equals("cacador") || level < 15) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Apenas Caçadores de nível 15+ podem comprar Pérolas do Ender!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        ItemStack result = event.getRecipe().getResult();
        Material itemType = result.getType();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();
        ClassDefinition classDef = availableClasses.get(className);

        // Permitir que qualquer classe crie ferramentas de madeira e pedra
        if (itemType.name().contains("WOODEN_") || itemType.name().contains("STONE_")) {
            return; // Permitir sem restrições
        }

        if (className.equals("ferreiro") && isForgeableItem(itemType)) {
            // Verificar cooldown
            if (estaNoCooldown(playerId)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você precisa descansar antes de forjar novamente!");
                return;
            }

            // Verificar permissão de nível para este item
            if (!hasRequiredLevelForCrafting(data, itemType)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não tem nível suficiente para forjar este item!");
                return;
            }

// Adicionar XP com multiplicador
            int baseXp = getXpForCrafting(itemType);
            int xpGained = baseXp;

// Aplicar multiplicador de XP se existir
            if (xpMultiplierSystem.hasActiveMultiplier(playerId)) {
                xpGained = xpMultiplierSystem.applyMultiplier(playerId, baseXp);
                int bonus = xpGained - baseXp;
                if (bonus > 0) {
                    player.sendMessage(ChatColor.AQUA + "+" + bonus + " XP bônus do multiplicador!");
                }
            }

            data.addXp(xpGained);
            player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Ferreiro");

            definirCooldown(playerId, 5); // 5 segundos

            player.sendMessage(ChatColor.GOLD + "Você se sente cansado após forjar. Aguarde 5 segundos antes de forjar novamente.");

            // Efeito visual e sonoro
            player.getWorld().spawnParticle(Particle.SMOKE,
                    player.getLocation().add(0, 1, 0),
                    10, 0.5, 0.5, 0.5, 0.02);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.5f);

            // Chance de criar um item melhorado
            if (data.getLevel() >= 10 && Math.random() < 0.3) {  // 30% de chance em nível 10+
                ItemStack enhancedItem = enhanceForgedItem(event.getRecipe().getResult(), data.getLevel());
                event.setCurrentItem(enhancedItem);
                player.sendMessage(ChatColor.GOLD + "Sua habilidade de ferreiro criou um item aprimorado!");
            }

            // Verificar level up
            checkLevelUp(player, data);

            // Atualizar scoreboard
            updateScoreboard(player);
        } else if (isForgeableItem(itemType) && !className.equals("ferreiro")) {
            // Penalidade para classes erradas
            if (Math.random() < 0.5) {  // 50% de chance de penalidade
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não possui habilidades suficientes para criar este item corretamente.");
            }

            // Verificar level up
            checkLevelUp(player, data);

            // Atualizar scoreboard
            updateScoreboard(player);

            if (isForgeableItem(itemType) && !className.equals("ferreiro")) {
                // SEMPRE bloquear - sem chance aleatória
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não possui habilidades suficientes para criar este item!");

                // Garantir que o jogador não pode obter o item
                event.setCurrentItem(null);

                // Fechar o inventário para o jogador (opcional, pode ser desconfortável)
                // player.closeInventory();

                // Aplicar um cooldown para evitar spam (opcional)
                player.setCooldown(Material.CRAFTING_TABLE, 20); // 1 segundo
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName().toLowerCase();
        ClassDefinition classDef = availableClasses.get(className);
        int level = data.getLevel();

        // Verificar se o item na mão é permitido para a classe
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null) {
            Material material = item.getType();

            // Permitir ferramentas básicas de madeira para todos
            if (material == Material.WOODEN_PICKAXE || material == Material.WOODEN_AXE) {
                return;

            }

            // Verificar restrições de nível para mineradores com picaretas avançadas
            if (className.equals("minerador")) {
                if ((material == Material.DIAMOND_PICKAXE && level < 15) ||
                        (material == Material.NETHERITE_PICKAXE && level < 25)) {

                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2));
                    player.sendMessage(ChatColor.RED + "Você não tem experiência suficiente para usar esta picareta avançada!");
                    player.sendMessage(ChatColor.YELLOW + "Nível necessário: " +
                            (material == Material.DIAMOND_PICKAXE ? "15" : "25"));
                }
            } else if (isRestrictedTool(material) && !classDef.isAllowedTool(material)) {
                // Aplicar efeito negativo para ferramentas de outras classes
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2));

                if (Math.random() < 0.3) { // 30% de chance
                    player.sendMessage(ChatColor.RED + "Você não está treinado para usar esta ferramenta eficientemente!");
                }
            }
        }
    }

    // Métodos auxiliares

    private boolean isMiningBlock(Material material) {
        String materialName = material.name().toLowerCase();
        return material.name().contains("ORE") ||
                material == Material.ANCIENT_DEBRIS ||
                material == Material.NETHER_GOLD_ORE ||
                material == Material.RAW_IRON_BLOCK ||
                material == Material.RAW_GOLD_BLOCK ||
                material == Material.RAW_COPPER_BLOCK;
    }

    private boolean isConstructionBlock(Material material) {
        return material.isBlock() && material.isSolid() &&
                !material.name().contains("ORE");
    }

    private boolean isHuntableEntity(EntityType entityType) {
        return entityType == EntityType.ZOMBIE ||
                entityType == EntityType.SKELETON ||
                entityType == EntityType.CREEPER ||
                entityType == EntityType.SPIDER ||
                entityType == EntityType.ENDERMAN ||
                entityType == EntityType.BLAZE ||
                entityType == EntityType.WITHER_SKELETON;
    }

    // Método para verificar todas as ferramentas inválidas de todos os jogadores
    private void checkInvalidToolsForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            PlayerClassData data = playerClasses.get(playerId);

            if (data == null || data.getClassName() == null) {
                continue;
            }

            String className = data.getClassName().toLowerCase();
            ClassDefinition classDef = availableClasses.get(className);

            // Verificar o item na mão principal
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null) {
                Material material = mainHand.getType();

                // Permitir ferramentas básicas de madeira para todos
                if (material == Material.WOODEN_PICKAXE || material == Material.WOODEN_AXE) {
                    continue;
                }

                if (isRestrictedTool(material) && !classDef.isAllowedTool(material)) {
                    // Aplicar efeito de fadiga de mineração (Mining Fatigue III) para 6 segundos
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 2, false, false, true));

                    // Enviar mensagem com uma chance baixa para não spammar o chat
                    if (Math.random() < 0.2) { // 20% de chance
                        player.sendMessage(ChatColor.RED + "Você não está treinado para usar esta ferramenta eficientemente!");
                    }
                }
            }
        }
    }

    private boolean isMonster(EntityType entityType) {
        return entityType.name().contains("ZOMBIE") ||
                entityType.name().contains("SKELETON") ||
                entityType == EntityType.CREEPER ||
                entityType == EntityType.SPIDER ||
                entityType == EntityType.ENDERMAN ||
                entityType == EntityType.BLAZE ||
                entityType == EntityType.WITCH ||
                entityType == EntityType.PHANTOM;
    }

    private boolean isHostileEntity(EntityType entityType) {
        return isMonster(entityType);
    }

    private boolean isForgeableItem(Material material) {
        return material.name().contains("PICKAXE") ||
                material.name().contains("SWORD") ||
                material.name().contains("AXE") ||
                material.name().contains("SHOVEL") ||
                material.name().contains("HOE") ||
                material.name().contains("ARMOR") ||
                material.name().contains("HELMET") ||
                material.name().contains("CHESTPLATE") ||
                material.name().contains("LEGGINGS") ||
                material.name().contains("BOOTS");
    }

    private boolean isRestrictedTool(Material material) {
        // Picaretas e machados de madeira não são restritos
        if (material == Material.WOODEN_PICKAXE || material == Material.WOODEN_AXE) {
            return false;
        }
        return material.name().contains("PICKAXE") ||
                material.name().contains("SWORD") ||
                material.name().contains("BOW") ||
                material.name().contains("CROSSBOW") ||
                material.name().contains("FISHING_ROD") ||
                material == Material.ANVIL ||
                material == Material.SMITHING_TABLE;
    }

    private boolean hasRequiredLevelForOre(PlayerClassData data, Material oreMaterial) {
        int playerLevel = data.getLevel();
        String oreName = oreMaterial.name().toLowerCase();


        if (oreName.contains("coal_ore")) {
            return playerLevel >= 1;
        } else if (oreName.contains("iron_ore") || oreName.equals("raw_iron_block")) {
            return playerLevel >= 4;
        } else if (oreName.contains("copper_ore") || oreName.equals("raw_copper_block")) {
            return playerLevel >= 3;
        } else if (oreName.contains("gold_ore") || oreMaterial == Material.NETHER_GOLD_ORE || oreName.equals("raw_gold_block")) {
            return playerLevel >= 5;
        } else if (oreName.contains("redstone_ore")) {
            return playerLevel >= 7;
        } else if (oreName.contains("lapis_ore")) {
            return playerLevel >= 12;
        } else if (oreName.contains("diamond_ore")) {
            return playerLevel >= 15;
        } else if (oreMaterial == Material.ANCIENT_DEBRIS) {
            return playerLevel >= 25;
        }

        return true;  // Por padrão, permite outros blocos
    }

    private boolean hasRequiredLevelForEntity(PlayerClassData data, EntityType entityType) {
        int playerLevel = data.getLevel();

        if (entityType == EntityType.ZOMBIE || entityType == EntityType.SKELETON) {
            return playerLevel >= 1;
        } else if (entityType == EntityType.SPIDER || entityType == EntityType.CREEPER) {
            return playerLevel >= 3;
        } else if (entityType == EntityType.WITCH || entityType == EntityType.PHANTOM) {
            return playerLevel >= 7;
        } else if (entityType == EntityType.ENDERMAN || entityType == EntityType.BLAZE) {
            return playerLevel >= 15;
        } else if (entityType == EntityType.WITHER_SKELETON) {
            return playerLevel >= 20;
        }

        return true;  // Por padrão, permite outras entidades
    }

    private boolean hasRequiredLevelForCrafting(PlayerClassData data, Material itemType) {
        int playerLevel = data.getLevel();

        if (itemType.name().contains("WOOD") || itemType.name().contains("STONE")) {
            return playerLevel >= 1;
        } else if (itemType.name().contains("IRON")) {
            return playerLevel >= 5;
        } else if (itemType.name().contains("GOLD")) {
            return playerLevel >= 10;
        } else if (itemType.name().contains("DIAMOND")) {
            return playerLevel >= 15;
        } else if (itemType.name().contains("NETHERITE")) {
            return playerLevel >= 20;
        }

        return true;  // Por padrão, permite outros itens
    }

    private int getXpForMining(Material oreMaterial) {
        String oreName = oreMaterial.name().toLowerCase();

        if (oreName.contains("coal_ore")) {
            return 5;
        } else if (oreName.contains("iron_ore")) {
            return 10;
        } else if (oreName.contains("copper_ore")) {
            return 4;
        } else if (oreName.contains("raw_copper_ore")) {
            return 7;
        } else if (oreName.contains("raw_iron_ore")) {
            return 15;
        } else if (oreName.contains("gold_ore") || oreMaterial == Material.NETHER_GOLD_ORE) {
            return 15;
        } else if (oreName.contains("raw_gold_ore")) {
            return 20;
        } else if (oreName.contains("redstone_ore")) {
            return 12;
        } else if (oreName.contains("lapis_ore")) {
            return 20;
        } else if (oreName.contains("diamond_ore")) {
            return 30;
        } else if (oreMaterial == Material.ANCIENT_DEBRIS) {
            return 50;
        }

        return 1;  // XP mínimo para outros blocos
    }

    private int getXpForKilling(EntityType entityType) {
        if (entityType == EntityType.ZOMBIE) {
            return 10;
        } else if (entityType == EntityType.SKELETON) {
            return 12;
        } else if (entityType == EntityType.SPIDER) {
            return 8;
        } else if (entityType == EntityType.CREEPER) {
            return 15;
        } else if (entityType == EntityType.WITCH) {
            return 20;
        } else if (entityType == EntityType.PHANTOM) {
            return 18;
        } else if (entityType == EntityType.ENDERMAN) {
            return 25;
        } else if (entityType == EntityType.BLAZE) {
            return 22;
        } else if (entityType == EntityType.WITHER_SKELETON) {
            return 30;
        }

        return 5;  // XP mínimo para outras entidades
    }

    private int getXpForCrafting(Material itemType) {
        if (itemType.name().contains("WOOD") || itemType.name().contains("STONE")) {
            return 5;
        } else if (itemType.name().contains("IRON")) {
            return 15;
        } else if (itemType.name().contains("GOLD")) {
            return 20;
        } else if (itemType.name().contains("DIAMOND")) {
            return 30;
        } else if (itemType.name().contains("NETHERITE")) {
            return 50;
        }

        return 2;  // XP mínimo para outros itens
    }

    private ItemStack getSpecialHunterDrop(EntityType entityType, int playerLevel) {
        if (entityType == EntityType.ZOMBIE) {
            return new ItemStack(Material.ROTTEN_FLESH, 3);
        } else if (entityType == EntityType.SKELETON) {
            return new ItemStack(Material.ARROW, 5);
        } else if (entityType == EntityType.CREEPER) {
            return new ItemStack(Material.GUNPOWDER, 3);
        } else if (entityType == EntityType.ENDERMAN && playerLevel >= 15) {
            return new ItemStack(Material.ENDER_PEARL, 2);
        } else if (entityType == EntityType.BLAZE && playerLevel >= 15) {
            return new ItemStack(Material.BLAZE_ROD, 2);
        } else if (entityType == EntityType.WITHER_SKELETON && playerLevel >= 20) {
            if (Math.random() < 0.3) {  // 30% de chance
                return new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
            } else {
                return new ItemStack(Material.COAL, 3);
            }
        }

        return null;
    }

    private ItemStack getSpecialFishingDrop(int playerLevel) {
        if (playerLevel < 10) {
            return new ItemStack(Material.COD, 3);
        } else if (playerLevel < 15) {
            return new ItemStack(Material.SALMON, 2);
        } else if (playerLevel < 20) {
            double chance = Math.random();
            if (chance < 0.6) {
                return new ItemStack(Material.TROPICAL_FISH, 1);
            } else if (chance < 0.9) {
                return new ItemStack(Material.PUFFERFISH, 1);
            } else {
                return new ItemStack(Material.NAME_TAG, 1);
            }
        } else {
            double chance = Math.random();
            if (chance < 0.4) {
                return new ItemStack(Material.NAUTILUS_SHELL, 1);
            } else if (chance < 0.7) {
                return new ItemStack(Material.HEART_OF_THE_SEA, 1);
            } else if (chance < 0.9) {
                return new ItemStack(Material.TRIDENT, 1);
            } else {
                return new ItemStack(Material.ENCHANTED_BOOK, 1);
            }
        }
    }

    private ItemStack enhanceForgedItem(ItemStack baseItem, int playerLevel) {
        ItemStack enhancedItem = baseItem.clone();
        // Aqui você implementaria a lógica para melhorar o item
        // Por exemplo, adicionando encantamentos ou atributos especiais

        // Este é um exemplo simples
        if (baseItem.getType().name().contains("SWORD")) {
            enhancedItem.addEnchantment(Enchantment.getByKey(NamespacedKey.minecraft("sharpness")),
                    Math.min(playerLevel / 5, 5));
        } else if (baseItem.getType().name().contains("PICKAXE")) {
            enhancedItem.addEnchantment(Enchantment.getByKey(NamespacedKey.minecraft("efficiency")),
                    Math.min(playerLevel / 5, 5));
        } else if (baseItem.getType().name().contains("ARMOR") ||
                baseItem.getType().name().contains("HELMET") ||
                baseItem.getType().name().contains("CHESTPLATE") ||
                baseItem.getType().name().contains("LEGGINGS") ||
                baseItem.getType().name().contains("BOOTS")) {
            enhancedItem.addEnchantment(Enchantment.getByKey(NamespacedKey.minecraft("protection")),
                    Math.min(playerLevel / 5, 4));
        }

        return enhancedItem;
    }

    public void checkLevelUp(Player player, PlayerClassData data) {
        int currentLevel = data.getLevel();
        String className = data.getClassName();
        ClassDefinition classDef = availableClasses.get(className.toLowerCase());

        int xpNeeded = classDef.getLevelRequirement(currentLevel + 1);
        if (xpNeeded > 0 && data.getXp() >= xpNeeded) {
            // Level Up!
            data.setLevel(currentLevel + 1);
            player.sendMessage(ChatColor.GOLD + "================================");
            player.sendMessage(ChatColor.GREEN + "LEVEL UP! Você subiu para " + className + " nível " + data.getLevel());
            player.sendMessage(ChatColor.YELLOW + "Novas habilidades desbloqueadas!");
            player.sendMessage(ChatColor.GOLD + "================================");

            // Efeitos de level up
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);

            // Aplicar permissões do novo nível
            List<String> newPerms = classDef.getLevelPermissions(data.getLevel());
            if (newPerms != null && !newPerms.isEmpty()) {
                for (String perm : newPerms) {
                    player.sendMessage(ChatColor.YELLOW + "- Desbloqueado: " + getPermissionDescription(perm));
                }
            }

            // Verificar se há mais level ups (caso tenha ganhado muito XP de uma vez)
            checkLevelUp(player, data);
        }
    }


    public String getPermissionDescription(String permission) {
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

    public void updateScoreboard(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }

        String className = data.getClassName();
        int level = data.getLevel();
        int xp = data.getXp();

        ClassDefinition classDef = availableClasses.get(className.toLowerCase());
        int nextLevelXp = classDef.getLevelRequirement(level + 1);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("classInfo", "dummy",
                ChatColor.GOLD + className + " " + ChatColor.YELLOW + "Nível " + level);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score xpScore = objective.getScore(ChatColor.GREEN + "XP: " + xp + "/" + nextLevelXp);
        xpScore.setScore(5);


        player.setScoreboard(board);
    }

    private void updateVisualEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            PlayerClassData data = playerClasses.get(playerId);

            if (data != null && data.getClassName() != null) {
                String className = data.getClassName().toLowerCase();
                int level = data.getLevel();
                ClassDefinition classDef = availableClasses.get(className);

                // Aplicar efeitos visuais baseados no nível
                Particle effect = classDef.getParticleEffect(level);
                if (effect != null && Math.random() < 0.3) {  // 30% de chance por tick para não sobrecarregar
                    player.getWorld().spawnParticle(effect, player.getLocation().add(0, 1, 0),
                            3, 0.2, 0.2, 0.2, 0.02);

                    if (level >= 15 && player.getLocation().getY() < 30) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 0, true, false));
                    }


                }

                // Aplicar buffs específicos de classe (exemplo)
                if (className.equals("minerador") && level >= 10) {
                    // Minerador de alto nível: visão noturna nas cavernas
                    if (player.getLocation().getY() < 40) {  // Abaixo do nível 40 (nas profundezas)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, true, false));

                    }
                } else if (className.equals("cacador") && level >= 20) {
                    // Caçador de alto nível: velocidade aumentada à noite
                    if (player.getWorld().getTime() > 13000 && player.getWorld().getTime() < 23000) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false));
                    }
                } else if (className.equals("guerreiro") && level >= 15) {
                    // Guerreiro de alto nível: resistência passiva
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, true, false));
                }
            }
        }
    }

    // Gerenciamento de dados dos jogadores

    private void loadPlayerData(UUID playerId) {
        File playerFile = new File(playerDataFolder, playerId.toString() + ".yml");
        if (playerFile.exists()) {
            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            String className = playerConfig.getString("class");
            int level = playerConfig.getInt("level", 1);
            int xp = playerConfig.getInt("xp", 0);
            long resetCooldownEnd = playerConfig.getLong("reset_cooldown", 0);

            PlayerClassData data = new PlayerClassData(className, level, xp, resetCooldownEnd);
            playerClasses.put(playerId, data);
        } else {
            // Jogador novo, sem dados
            playerClasses.put(playerId, new PlayerClassData(null, 1, 0));
        }
    }

    public void savePlayerData(UUID playerId) {
        PlayerClassData data = playerClasses.get(playerId);
        if (data == null) {
            return;
        }

        File playerFile = new File(playerDataFolder, playerId.toString() + ".yml");
        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        playerConfig.set("class", data.getClassName());
        playerConfig.set("level", data.getLevel());
        playerConfig.set("xp", data.getXp());
        playerConfig.set("reset_cooldown", data.getResetCooldownEnd());

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            getLogger().severe("Não foi possível salvar dados para o jogador " + playerId);
            e.printStackTrace();
        }
    }

    public void setPlayerClass(UUID playerId, String className) {
        PlayerClassData data = playerClasses.get(playerId);
        boolean firstTimeChoosingClass = false;

        if (data == null) {
            data = new PlayerClassData(className, 1, 0);
            playerClasses.put(playerId, data);
            firstTimeChoosingClass = true;
        } else {
            // Se já tem uma classe, verifica se é uma troca ou primeira escolha
            if (data.getClassName() != null) {
                // É uma troca de classe - reseta completamente o progresso e mantém o cooldown
                long cooldownEnd = data.getResetCooldownEnd(); // Preservar cooldown existente
                data.setClassName(className);
                data.setLevel(1); // Reseta para nível 1
                data.setXp(0);    // Zera o XP
                data.setResetCooldownEnd(cooldownEnd); // Restaura o cooldown
            } else {
                // Primeira vez escolhendo classe
                data.setClassName(className);
                firstTimeChoosingClass = true;
            }
        }

        // Incrementar contador apenas se for a primeira vez que escolhe uma classe
        if (firstTimeChoosingClass) {
            // Incrementar contador para a classe escolhida
            String classKey = className.toLowerCase();
            int currentCount = classCounters.getOrDefault(classKey, 0);
            classCounters.put(classKey, currentCount + 1);
            saveClassCounters();
        }

        // Salvar imediatamente a alteração
        savePlayerData(playerId);

        // Atualizar scoreboard se o jogador estiver online
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            updateScoreboard(player);
        }
    }

    public int getClassCount(String className) {
        return classCounters.getOrDefault(className.toLowerCase(), 0);
    }

    public Map<String, Integer> getClassCounters() {
        return new HashMap<>(classCounters); // Retorna uma cópia para evitar modificações diretas
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        config = getConfig();
    }


    }
