package com.minecraft.classesplugin;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private Map<UUID, PlayerClassData> playerClasses;
    private Map<String, ClassDefinition> availableClasses;
    private File playerDataFolder;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Inicialização do plugin
        saveDefaultConfig();
        config = getConfig();
        
        // Criar pasta para dados de jogadores
        playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
        
        // Inicializar mapas
        playerClasses = new HashMap<>();
        availableClasses = new HashMap<>();
        
        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);
        
        // Definir classes disponíveis
        initializeClasses();
        
        // Comandos
        getCommand("classe").setExecutor(new ClassCommandHandler(this));
        getCommand("missao").setExecutor(new QuestCommandHandler(this));
        getCommand("classeadmin").setExecutor(new ClassCommandHandlerAdmin(this));
        
        // Agendador para efeitos visuais e atualizações
        Bukkit.getScheduler().runTaskTimer(this, () -> updateVisualEffects(), 20L, 20L);
        
        getLogger().info("Sistema de Classes ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        // Salvar dados dos jogadores
        for (UUID playerId : playerClasses.keySet()) {
            savePlayerData(playerId);
        }
        getLogger().info("Sistema de Classes desativado. Dados salvos.");
    }

    // Inicialização das definições de classes
    private void initializeClasses() {
        // Classe Minerador
        ClassDefinition minerador = new ClassDefinition("Minerador");
        minerador.addLevelRequirement(1, 100);
        minerador.addLevelRequirement(2, 250);
        minerador.addLevelRequirement(3, 450);
        // ... mais níveis

        // Habilidades desbloqueáveis do Minerador
        minerador.addLevelPermission(1, "classes.minerador.carvao");
        minerador.addLevelPermission(2, "classes.minerador.ferro");
        minerador.addLevelPermission(5, "classes.minerador.ouro");
        minerador.addLevelPermission(10, "classes.minerador.redstone");
        minerador.addLevelPermission(15, "classes.minerador.lapislazuli");
        minerador.addLevelPermission(20, "classes.minerador.diamante");
        minerador.addLevelPermission(30, "classes.minerador.netherite");
        
        // Missões do Minerador
        minerador.addQuest(new Quest("mine_coal", "Extraia 30 minérios de carvão", 30, QuestType.MINING, Material.COAL_ORE, 100));
        minerador.addQuest(new Quest("mine_iron", "Extraia 50 minérios de ferro", 50, QuestType.MINING, Material.IRON_ORE, 250));
        minerador.addQuest(new Quest("mine_gold", "Extraia 30 minérios de ouro", 30, QuestType.MINING, Material.GOLD_ORE, 300));
        minerador.addQuest(new Quest("mine_diamond", "Extraia 15 minérios de diamante", 15, QuestType.MINING, Material.DIAMOND_ORE, 500));
        
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
        cacador.addLevelRequirement(3, 450);
        // ... mais níveis
        
        cacador.addLevelPermission(1, "classes.cacador.animais");
        cacador.addLevelPermission(3, "classes.cacador.monstros");
        cacador.addLevelPermission(7, "classes.cacador.rastrear");
        cacador.addLevelPermission(10, "classes.cacador.armadilhas");
        cacador.addLevelPermission(15, "classes.cacador.pets");
        cacador.addLevelPermission(20, "classes.cacador.bosses");
        
        cacador.addQuest(new Quest("kill_zombies", "Mate 20 zumbis", 20, QuestType.KILLING, EntityType.ZOMBIE, 150));
        cacador.addQuest(new Quest("kill_skeletons", "Mate 15 esqueletos à noite", 15, QuestType.KILLING, EntityType.SKELETON, 200));
        cacador.addQuest(new Quest("kill_creepers", "Mate 10 creepers", 10, QuestType.KILLING, EntityType.CREEPER, 300));
        
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
        pescador.addLevelRequirement(2, 200);
        pescador.addLevelRequirement(3, 350);
        // ... mais níveis
        
        pescador.addLevelPermission(1, "classes.pescador.peixes");
        pescador.addLevelPermission(5, "classes.pescador.tesouros");
        pescador.addLevelPermission(10, "classes.pescador.raros");
        pescador.addLevelPermission(15, "classes.pescador.locaisespeciais");
        pescador.addLevelPermission(20, "classes.pescador.magicos");
        
        pescador.addQuest(new Quest("fish_basic", "Pesque 20 peixes comuns", 20, QuestType.FISHING, Material.COD, 150));
        pescador.addQuest(new Quest("fish_salmon", "Pesque 15 salmões", 15, QuestType.FISHING, Material.SALMON, 200));
        pescador.addQuest(new Quest("fish_pufferfish", "Pesque 5 baiacus", 5, QuestType.FISHING, Material.PUFFERFISH, 250));
        
        pescador.addAllowedTool(Material.FISHING_ROD);
        
        pescador.setParticleEffect(1, Particle.BUBBLE);
        pescador.setParticleEffect(10, Particle.SPLASH);
        pescador.setParticleEffect(20, Particle.DOLPHIN);
        
        availableClasses.put("pescador", pescador);
        
        // Classe Guerreiro
        ClassDefinition guerreiro = new ClassDefinition("Guerreiro");
        guerreiro.addLevelRequirement(1, 100);
        guerreiro.addLevelRequirement(2, 250);
        guerreiro.addLevelRequirement(3, 450);
        // ... mais níveis
        
        guerreiro.addLevelPermission(1, "classes.guerreiro.basico");
        guerreiro.addLevelPermission(5, "classes.guerreiro.armaduras");
        guerreiro.addLevelPermission(10, "classes.guerreiro.tecnicas");
        guerreiro.addLevelPermission(15, "classes.guerreiro.resistencia");
        guerreiro.addLevelPermission(20, "classes.guerreiro.escudos");
        
        guerreiro.addQuest(new Quest("win_duel", "Ganhe um duelo contra outro jogador", 1, QuestType.DUEL, null, 300));
        guerreiro.addQuest(new Quest("kill_monsters", "Mate 50 monstros", 50, QuestType.KILLING_ANY_MONSTER, null, 400));
        
        guerreiro.addAllowedTool(Material.WOODEN_SWORD);
        guerreiro.addAllowedTool(Material.STONE_SWORD);
        guerreiro.addAllowedTool(Material.IRON_SWORD);
        guerreiro.addAllowedTool(Material.GOLDEN_SWORD);
        guerreiro.addAllowedTool(Material.DIAMOND_SWORD);
        guerreiro.addAllowedTool(Material.NETHERITE_SWORD);
        guerreiro.addAllowedTool(Material.SHIELD);
        
        guerreiro.setParticleEffect(1, Particle.CRIT);
        guerreiro.setParticleEffect(10, Particle.SWEEP_ATTACK);
        guerreiro.setParticleEffect(20, Particle.FLAME);
        
        availableClasses.put("guerreiro", guerreiro);
        
        // Classe Ferreiro
        ClassDefinition ferreiro = new ClassDefinition("Ferreiro");
        ferreiro.addLevelRequirement(1, 100);
        ferreiro.addLevelRequirement(2, 250);
        ferreiro.addLevelRequirement(3, 400);
        // ... mais níveis
        
        ferreiro.addLevelPermission(1, "classes.ferreiro.ferramentas.basicas");
        ferreiro.addLevelPermission(5, "classes.ferreiro.armas.basicas");
        ferreiro.addLevelPermission(10, "classes.ferreiro.ferramentas.avancadas");
        ferreiro.addLevelPermission(15, "classes.ferreiro.armas.avancadas");
        ferreiro.addLevelPermission(20, "classes.ferreiro.lendarias");
        
        ferreiro.addQuest(new Quest("craft_pickaxe", "Forje uma picareta de ferro", 1, QuestType.CRAFTING, Material.IRON_PICKAXE, 150));
        ferreiro.addQuest(new Quest("craft_sword", "Forje uma espada de diamante", 1, QuestType.CRAFTING, Material.DIAMOND_SWORD, 300));
        
        ferreiro.addAllowedTool(Material.ANVIL);
        ferreiro.addAllowedTool(Material.SMITHING_TABLE);
        
        ferreiro.setParticleEffect(1, Particle.LAVA);
        ferreiro.setParticleEffect(10, Particle.FLAME);
        ferreiro.setParticleEffect(20, Particle.LARGE_SMOKE);
        
        availableClasses.put("ferreiro", ferreiro);
        
        // Classe Artesão/Construtor
        ClassDefinition artesao = new ClassDefinition("Artesão");
        artesao.addLevelRequirement(1, 100);
        artesao.addLevelRequirement(2, 200);
        artesao.addLevelRequirement(3, 350);
        // ... mais níveis
        
        artesao.addLevelPermission(1, "classes.artesao.blocos.basicos");
        artesao.addLevelPermission(5, "classes.artesao.decoracao");
        artesao.addLevelPermission(10, "classes.artesao.estruturas.pequenas");
        artesao.addLevelPermission(15, "classes.artesao.estruturas.medias");
        artesao.addLevelPermission(20, "classes.artesao.estruturas.grandes");
        
        artesao.addQuest(new Quest("craft_decorative", "Crie 20 blocos decorativos", 20, QuestType.CRAFTING_ANY_DECORATIVE, null, 150));
        artesao.addQuest(new Quest("build_house", "Construa uma casa pequena", 1, QuestType.CONSTRUCTION, null, 300));
        
        artesao.addAllowedTool(Material.CRAFTING_TABLE);
        artesao.addAllowedTool(Material.STONECUTTER);
        artesao.addAllowedTool(Material.LOOM);
        
        artesao.setParticleEffect(1, Particle.HEART);
        artesao.setParticleEffect(10, Particle.COMPOSTER);
        artesao.setParticleEffect(20, Particle.SOUL);
        
        availableClasses.put("artesao", artesao);
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
            player.sendMessage(ChatColor.AQUA + "Classes disponíveis: Minerador, Caçador, Pescador, Guerreiro, Ferreiro, Artesão");
        }
    }
    
    @EventHandler
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
        
        // Verificar se tem permissão para quebrar este bloco
        if (className.equals("minerador") && isMiningBlock(blockType)) {
            // Verificar se tem o nível necessário para este minério
            if (!hasRequiredLevelForOre(data, blockType)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não tem nível suficiente para minerar este recurso!");
                return;
            }
            
            // Verificar se está usando a ferramenta certa
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!classDef.isAllowedTool(tool.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você precisa de uma ferramenta de minerador adequada!");
                return;
            }
            
            // Adicionar XP e verificar missões
            int xpGained = getXpForMining(blockType);
            data.addXp(xpGained);
            player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Minerador");
            
            // Atualizar missões
            for (Quest quest : classDef.getQuests()) {
                if (quest.getType() == QuestType.MINING && quest.getTargetMaterial() == blockType) {
                    data.incrementQuestProgress(quest.getId());
                    int progress = data.getQuestProgress(quest.getId());
                    int target = quest.getTargetAmount();
                    
                    if (progress >= target && !data.isQuestCompleted(quest.getId())) {
                        // Completou a missão
                        data.completeQuest(quest.getId());
                        int questXp = quest.getRewardXp();
                        data.addXp(questXp);
                        player.sendMessage(ChatColor.GOLD + "Missão concluída: " + quest.getDescription());
                        player.sendMessage(ChatColor.GREEN + "+" + questXp + " XP de Minerador");
                        
                        // Efeitos visuais e sonoros
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        player.spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                    } else if (progress % 5 == 0) {
                        // Atualização de progresso a cada 5 unidades
                        player.sendMessage(ChatColor.YELLOW + "Progresso na missão: " + 
                                           progress + "/" + target + " - " + quest.getDescription());
                    }
                }
            }
            
            // Verificar level up
            checkLevelUp(player, data);
            
            // Atualizar scoreboard
            updateScoreboard(player);
            
        } else if (className.equals("artesao") && isConstructionBlock(blockType)) {
            // Lógica similar para o Artesão
            // ...
            
        } else if (isMiningBlock(blockType) && !className.equals("minerador")) {
            // Penalidade para classe errada tentando minerar
            if (Math.random() < 0.7) {  // 70% de chance de penalidade
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Como você não é um Minerador, não conseguiu extrair este minério corretamente.");
                event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
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
            
            // Adicionar XP
            int xpGained = getXpForKilling(entityType);
            data.addXp(xpGained);
            player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Caçador");
            
            // Atualizar missões
            for (Quest quest : classDef.getQuests()) {
                if (quest.getType() == QuestType.KILLING && quest.getEntityTarget() == entityType) {
                    data.incrementQuestProgress(quest.getId());
                    int progress = data.getQuestProgress(quest.getId());
                    int target = quest.getTargetAmount();
                    
                    if (progress >= target && !data.isQuestCompleted(quest.getId())) {
                        // Completou a missão
                        data.completeQuest(quest.getId());
                        int questXp = quest.getRewardXp();
                        data.addXp(questXp);
                        player.sendMessage(ChatColor.GOLD + "Missão concluída: " + quest.getDescription());
                        player.sendMessage(ChatColor.GREEN + "+" + questXp + " XP de Caçador");
                        
                        // Efeitos
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                    } else if (progress % 5 == 0) {
                        player.sendMessage(ChatColor.YELLOW + "Progresso na missão: " + 
                                           progress + "/" + target + " - " + quest.getDescription());
                    }
                } else if (quest.getType() == QuestType.KILLING_ANY_MONSTER && isMonster(entityType)) {
                    // Missão de matar qualquer monstro
                    data.incrementQuestProgress(quest.getId());
                    // Restante da lógica similar...
                }
            }
            
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
            
        } else if (className.equals("guerreiro") && isHostileEntity(entityType)) {
            // Lógica similar para o Guerreiro
            // ...
            
        } else if (isHostileEntity(entityType) && !className.equals("cacador") && !className.equals("guerreiro")) {
            // Menos XP para classes não especializadas (sem penalidade completa)
            int baseXp = getXpForKilling(entityType);
            int reducedXp = baseXp / 3;  // 1/3 do XP normal
            
            if (reducedXp > 0) {
                data.addXp(reducedXp);
                player.sendMessage(ChatColor.YELLOW + "+" + reducedXp + " XP de " + data.getClassName() + 
                                   " (reduzido por não ser sua especialidade)");
                
                // Verificar level up
                checkLevelUp(player, data);
                
                // Atualizar scoreboard
                updateScoreboard(player);
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
        ClassDefinition classDef = availableClasses.get(className);
        
        if (className.equals("pescador") && event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Verificar se está usando uma vara de pesca
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!classDef.isAllowedTool(tool.getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você precisa de uma vara de pesca adequada!");
                return;
            }
            
            // Adicionar XP
            int xpGained = 20;  // XP base para pesca
            data.addXp(xpGained);
            player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Pescador");
            
            // Verificar que tipo de peixe foi pescado (se for o caso)
            Entity caught = event.getCaught();
            if (caught != null) {
                // Aqui você pode adicionar lógica para identificar o tipo de pesca
                // e atualizar missões específicas
            }
            
            // Chance de pesca especial baseada no nível
            if (data.getLevel() >= 10 && Math.random() < 0.15) {  // 15% de chance em nível 10+
                ItemStack specialCatch = getSpecialFishingDrop(data.getLevel());
                if (specialCatch != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), specialCatch);
                    player.sendMessage(ChatColor.GOLD + "Sua habilidade de pescador lhe concedeu um item especial!");
                }
            }
            
            // Verificar level up
            checkLevelUp(player, data);
            
            // Atualizar scoreboard
            updateScoreboard(player);
            
        } else if (!className.equals("pescador") && event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Penalidade para não-pescadores
            if (Math.random() < 0.6) {  // 60% de chance de penalidade
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "O peixe escapou! Você não tem habilidades de pesca suficientes.");
            }
        }
    }
    
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        Material itemType = event.getRecipe().getResult().getType();
        
        PlayerClassData data = playerClasses.get(playerId);
        if (data == null || data.getClassName() == null) {
            return;
        }
        
        String className = data.getClassName().toLowerCase();
        ClassDefinition classDef = availableClasses.get(className);
        
        if (className.equals("ferreiro") && isForgeableItem(itemType)) {
            // Verificar permissão de nível para este item
            if (!hasRequiredLevelForCrafting(data, itemType)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não tem nível suficiente para forjar este item!");
                return;
            }

            // Adicionar XP
            int xpGained = getXpForCrafting(itemType);
            data.addXp(xpGained);
            player.sendMessage(ChatColor.GREEN + "+" + xpGained + " XP de Ferreiro");
            
            // Atualizar missões
            for (Quest quest : classDef.getQuests()) {
                if (quest.getType() == QuestType.CRAFTING && quest.getTargetMaterial() == itemType) {
                    data.incrementQuestProgress(quest.getId());
                    int progress = data.getQuestProgress(quest.getId());
                    int target = quest.getTargetAmount();
                    
                    if (progress >= target && !data.isQuestCompleted(quest.getId())) {
                        // Completou a missão
                        data.completeQuest(quest.getId());
                        int questXp = quest.getRewardXp();
                        data.addXp(questXp);
                        player.sendMessage(ChatColor.GOLD + "Missão concluída: " + quest.getDescription());
                        player.sendMessage(ChatColor.GREEN + "+" + questXp + " XP de Ferreiro");
                        
                        // Efeitos
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        player.spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                    } else if (progress % 5 == 0) {
                        player.sendMessage(ChatColor.YELLOW + "Progresso na missão: " + 
                                          progress + "/" + target + " - " + quest.getDescription());
                    }
                }
            }
            
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
            
        } else if (className.equals("artesao") && isArtisanItem(itemType)) {
            // Lógica similar para o Artesão
            // ...
            
        } else if ((isForgeableItem(itemType) && !className.equals("ferreiro")) || 
                  (isArtisanItem(itemType) && !className.equals("artesao"))) {
            // Penalidade para classes erradas
            if (Math.random() < 0.5) {  // 50% de chance de penalidade
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não possui habilidades suficientes para criar este item corretamente.");
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
        
        // Verificar se o item na mão é permitido para a classe
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null && isRestrictedTool(item.getType()) && !classDef.isAllowedTool(item.getType())) {
            // Aplicar efeito negativo para ferramentas de outras classes
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2));
            player.sendMessage(ChatColor.RED + "Você não está treinado para usar esta ferramenta eficientemente!");
        }
    }
    
    // Métodos auxiliares
    
    private boolean isMiningBlock(Material material) {
        return material.name().contains("ORE") || 
              material == Material.ANCIENT_DEBRIS ||
              material == Material.NETHER_GOLD_ORE;
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
    
    private boolean isArtisanItem(Material material) {
        return material.name().contains("BANNER") ||
              material.name().contains("BED") ||
              material.name().contains("CARPET") ||
              material.name().contains("GLASS") ||
              material.name().contains("TERRACOTTA") ||
              material.name().contains("CONCRETE") ||
              material.name().contains("WOOL") ||
              material.name().contains("PLANKS") ||
              material.name().contains("STAIRS") ||
              material.name().contains("SLAB");
    }
    
    private boolean isRestrictedTool(Material material) {
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
        
        if (oreMaterial == Material.COAL_ORE) {
            return playerLevel >= 1;
        } else if (oreMaterial == Material.IRON_ORE) {
            return playerLevel >= 2;
        } else if (oreMaterial == Material.GOLD_ORE || oreMaterial == Material.NETHER_GOLD_ORE) {
            return playerLevel >= 5;
        } else if (oreMaterial == Material.REDSTONE_ORE) {
            return playerLevel >= 10;
        } else if (oreMaterial == Material.LAPIS_ORE) {
            return playerLevel >= 15;
        } else if (oreMaterial == Material.DIAMOND_ORE) {
            return playerLevel >= 20;
        } else if (oreMaterial == Material.ANCIENT_DEBRIS) {
            return playerLevel >= 30;
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
        if (oreMaterial == Material.COAL_ORE) {
            return 5;
        } else if (oreMaterial == Material.IRON_ORE) {
            return 10;
        } else if (oreMaterial == Material.GOLD_ORE || oreMaterial == Material.NETHER_GOLD_ORE) {
            return 15;
        } else if (oreMaterial == Material.REDSTONE_ORE) {
            return 12;
        } else if (oreMaterial == Material.LAPIS_ORE) {
            return 20;
        } else if (oreMaterial == Material.DIAMOND_ORE) {
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
        
        // Mostrar missões ativas
        int scorePos = 4;
        boolean hasActiveQuests = false;
        for (Quest quest : classDef.getQuests()) {
            if (!data.isQuestCompleted(quest.getId())) {
                int progress = data.getQuestProgress(quest.getId());
                int target = quest.getTargetAmount();
                
                if (progress > 0) {
                    hasActiveQuests = true;
                    Score questScore = objective.getScore(ChatColor.YELLOW + quest.getDescription() + 
                                                         ": " + progress + "/" + target);
                    questScore.setScore(scorePos--);
                    
                    if (scorePos < 1) break;  // Limitar número de missões exibidas
                }
            }
        }
        
        if (!hasActiveQuests) {
            Score noQuestScore = objective.getScore(ChatColor.YELLOW + "Nenhuma missão ativa");
            noQuestScore.setScore(4);
        }
        
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
                }
                
                // Aplicar buffs específicos de classe (exemplo)
                if (className.equals("minerador") && level >= 15) {
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
            
            PlayerClassData data = new PlayerClassData(className, level, xp);
            
            // Carregar progresso das missões
            if (playerConfig.contains("quests")) {
                for (String questId : playerConfig.getConfigurationSection("quests.progress").getKeys(false)) {
                    int progress = playerConfig.getInt("quests.progress." + questId, 0);
                    data.setQuestProgress(questId, progress);
                }
                
                for (String questId : playerConfig.getStringList("quests.completed")) {
                    data.completeQuest(questId);
                }
            }
            
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
        
        // Salvar progresso das missões
        for (Map.Entry<String, Integer> entry : data.getQuestProgress().entrySet()) {
            playerConfig.set("quests.progress." + entry.getKey(), entry.getValue());
        }
        
        playerConfig.set("quests.completed", new ArrayList<>(data.getCompletedQuests()));
        
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            getLogger().severe("Não foi possível salvar dados para o jogador " + playerId);
            e.printStackTrace();
        }
    }
    
    // Getters para uso em outros handlers
    
    public Map<UUID, PlayerClassData> getPlayerClasses() {
        return playerClasses;
    }
    
    public Map<String, ClassDefinition> getAvailableClasses() {
        return availableClasses;
    }
    
    public void setPlayerClass(UUID playerId, String className) {
        PlayerClassData data = playerClasses.get(playerId);
        if (data == null) {
            data = new PlayerClassData(className, 1, 0);
            playerClasses.put(playerId, data);
        } else {
            // Se já tem uma classe, resetar com penalidade
            if (data.getClassName() != null) {
                int oldLevel = data.getLevel();
                // Reset com penalidade de 50% do progresso
                data.setClassName(className);
                data.setLevel(Math.max(1, oldLevel / 2));
                data.setXp(0);
                data.clearQuestProgress();
            } else {
                // Primeira vez escolhendo classe
                data.setClassName(className);
            }
        }
        
        // Salvar imediatamente a alteração
        savePlayerData(playerId);
        
        // Atualizar scoreboard se o jogador estiver online
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            updateScoreboard(player);


        }
    }
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        config = getConfig();
    }
}
