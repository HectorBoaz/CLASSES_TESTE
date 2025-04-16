package com.minecraft.classesplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

/**
 * Sistema de GUI para seleção e gerenciamento de classes
 */
public class ClassSelectionGUI implements Listener {

    private static Main plugin = null;
    private static final Map<UUID, BukkitTask> forcedSelectionTasks = new HashMap<>();
    private static final long RESET_COOLDOWN = 86400000; // 24 horas em milissegundos

    public ClassSelectionGUI(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Abre o menu de seleção de classe para o jogador
     *
     * @param player Jogador para abrir o menu
     * @param forced Se true, força o jogador a escolher uma classe (não pode fechar o menu)
     */
    public static void openClassSelectionMenu(Player player, boolean forced) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Escolha sua Classe");

        // Obter contadores de jogadores por classe
        int mineradorCount = plugin.getClassCount("minerador");
        int cacadorCount = plugin.getClassCount("cacador");
        int pescadorCount = plugin.getClassCount("pescador");
        int ferreiroCount = plugin.getClassCount("ferreiro");

        // Item para Minerador
        ItemStack mineradorItem = createClassItem(
                Material.DIAMOND_PICKAXE,
                ChatColor.AQUA + "Minerador",
                Arrays.asList(
                        ChatColor.GRAY + "Especialista em exploração e mineração.",
                        ChatColor.GRAY + "Ganha XP extraindo recursos do mundo.",
                        "",
                        ChatColor.YELLOW + "• Habilidade com vários tipos de minérios",
                        ChatColor.YELLOW + "• Especialista em ferramentas de mineração",
                        ChatColor.YELLOW + "• Descobre recursos raros e valiosos",
                        "",
                        ChatColor.LIGHT_PURPLE + "" + mineradorCount + " jogadores escolheram esta classe",
                        "",
                        ChatColor.GREEN + "Clique para selecionar esta classe!"
                )
        );

        // Item para Caçador
        ItemStack cacadorItem = createClassItem(
                Material.BOW,
                ChatColor.RED + "Caçador",
                Arrays.asList(
                        ChatColor.GRAY + "Especialista em combate e sobrevivência.",
                        ChatColor.GRAY + "Ganha XP enfrentando criaturas diversas.",
                        "",
                        ChatColor.YELLOW + "• Domina técnicas de combate e caça",
                        ChatColor.YELLOW + "• Obtém recompensas especiais de inimigos",
                        ChatColor.YELLOW + "• Habilidades táticas e de rastreamento",
                        "",
                        ChatColor.LIGHT_PURPLE + "" + cacadorCount + " jogadores escolheram esta classe",
                        "",
                        ChatColor.GREEN + "Clique para selecionar esta classe!"
                )
        );

        // Item para Pescador
        ItemStack pescadorItem = createClassItem(
                Material.FISHING_ROD,
                ChatColor.BLUE + "Pescador",
                Arrays.asList(
                        ChatColor.GRAY + "Mestre das águas e seus tesouros.",
                        ChatColor.GRAY + "Ganha XP aproveitando os recursos aquáticos.",
                        "",
                        ChatColor.YELLOW + "• Pesca de itens comuns a extremamente raros",
                        ChatColor.YELLOW + "• Encontra tesouros submersos valiosos",
                        ChatColor.YELLOW + "• Habilidades únicas relacionadas à água",
                        "",
                        ChatColor.LIGHT_PURPLE + "" + pescadorCount + " jogadores escolheram esta classe",
                        "",
                        ChatColor.GREEN + "Clique para selecionar esta classe!"
                )
        );

        // Item para Ferreiro
        ItemStack ferreiroItem = createClassItem(
                Material.ANVIL,
                ChatColor.GOLD + "Ferreiro",
                Arrays.asList(
                        ChatColor.GRAY + "Artesão de armas, armaduras e ferramentas.",
                        ChatColor.GRAY + "Ganha XP criando e aprimorando equipamentos.",
                        "",
                        ChatColor.YELLOW + "• Cria e melhora itens com maestria",
                        ChatColor.YELLOW + "• Domina técnicas avançadas de encantamento",
                        ChatColor.YELLOW + "• Especialista em restauração e modificação",
                        "",
                        ChatColor.LIGHT_PURPLE + "" + ferreiroCount + " jogadores escolheram esta classe",
                        "",
                        ChatColor.GREEN + "Clique para selecionar esta classe!"
                )
        );

        // Colocar itens no inventário
        menu.setItem(10, mineradorItem);
        menu.setItem(12, cacadorItem);
        menu.setItem(14, pescadorItem);
        menu.setItem(16, ferreiroItem);

        // Preencher com vidro para decoração
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, glass);
            }
        }

        player.openInventory(menu);

        // Se for seleção forçada, impede que o jogador feche o menu
        if (forced) {
            if (forcedSelectionTasks.containsKey(player.getUniqueId())) {
                forcedSelectionTasks.get(player.getUniqueId()).cancel();
            }

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.getOpenInventory().getTitle().equals(ChatColor.DARK_PURPLE + "Escolha sua Classe")) {
                    openClassSelectionMenu(player, true);
                    player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe para jogar!");
                }
            }, 1L, 20L);

            forcedSelectionTasks.put(player.getUniqueId(), task);
        }
    }

    /**
     * Cria um item para o menu de seleção de classe
     */
    private static ItemStack createClassItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Abre o menu de confirmação para trocar de classe
     */
    public void openTrocaClasseConfirmationMenu(Player player) {
        // Verificar se o jogador está no cooldown
        UUID playerId = player.getUniqueId();
        PlayerClassData data = plugin.getPlayerClasses().get(playerId);

        if (data.isOnResetCooldown()) {
            long remainingTime = data.getRemainingResetCooldown();
            player.sendMessage(ChatColor.RED + "Você precisa esperar mais " + formatTime(remainingTime) +
                    " para poder trocar de classe novamente!");
            return;
        }

        // Verificar se o jogador tem uma classe
        if (!plugin.getPlayerClasses().containsKey(playerId) ||
                plugin.getPlayerClasses().get(playerId).getClassName() == null) {
            player.sendMessage(ChatColor.RED + "Você ainda não escolheu uma classe!");
            return;
        }

        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.RED + "Trocar de Classe?");

        // Item para cancelar
        ItemStack cancelItem = createClassItem(
                Material.RED_CONCRETE,
                ChatColor.RED + "Cancelar",
                Arrays.asList(
                        ChatColor.GRAY + "Manter sua classe atual.",
                        "",
                        ChatColor.YELLOW + "Clique para cancelar"
                )
        );

        // Item para confirmar
        ItemStack confirmItem = createClassItem(
                Material.LIME_CONCRETE,
                ChatColor.GREEN + "Confirmar",
                Arrays.asList(
                        ChatColor.GRAY + "Confirmar troca de classe.",
                        ChatColor.RED + "Isso abrirá uma tela final de confirmação.",
                        "",
                        ChatColor.YELLOW + "Clique para prosseguir"
                )
        );

        // Informações sobre a classe atual
        ItemStack infoItem = createClassItem(
                Material.BOOK,
                ChatColor.GOLD + "Informações",
                Arrays.asList(
                        ChatColor.GRAY + "Classe atual: " + ChatColor.YELLOW + data.getClassName(),
                        ChatColor.GRAY + "Nível: " + ChatColor.YELLOW + data.getLevel(),
                        ChatColor.GRAY + "XP: " + ChatColor.YELLOW + data.getXp(),
                        "",
                        ChatColor.RED + "ATENÇÃO: Trocar sua classe irá:",
                        ChatColor.RED + "• Zerar seu nível e todo progresso",
                        ChatColor.RED + "• Bloquear nova troca por 24h"
                )
        );

        // Colocar itens no inventário
        menu.setItem(11, cancelItem);
        menu.setItem(15, confirmItem);
        menu.setItem(13, infoItem);

        // Preencher com vidro para decoração
        ItemStack glass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, glass);
            }
        }

        player.openInventory(menu);
    }

    /**
     * Abre o menu de confirmação final para trocar de classe
     */
    public void openFinalTrocaClasseConfirmationMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "CONFIRMAÇÃO FINAL");

        // Item para cancelar
        ItemStack cancelItem = createClassItem(
                Material.RED_CONCRETE,
                ChatColor.RED + "CANCELAR",
                Arrays.asList(
                        ChatColor.GRAY + "MANTER SUA CLASSE ATUAL",
                        "",
                        ChatColor.YELLOW + "Clique para cancelar"
                )
        );

        // Item para confirmar
        ItemStack confirmItem = createClassItem(
                Material.LIME_CONCRETE,
                ChatColor.GREEN + "CONFIRMAR",
                Arrays.asList(
                        ChatColor.GRAY + "TROCAR SUA CLASSE",
                        "",
                        ChatColor.RED + "VOCÊ PERDERÁ TODO SEU PROGRESSO!",
                        ChatColor.RED + "ESTA AÇÃO NÃO PODE SER DESFEITA!",
                        ChatColor.RED + "Você terá que esperar 24h para",
                        ChatColor.RED + "trocar de classe novamente!",
                        "",
                        ChatColor.YELLOW + "Clique para confirmar"
                )
        );

        // Colocar itens no inventário
        menu.setItem(11, cancelItem);
        menu.setItem(15, confirmItem);

        // Item de aviso
        ItemStack warningItem = createClassItem(
                Material.BARRIER,
                ChatColor.DARK_RED + "⚠ ATENÇÃO ⚠",
                Arrays.asList(
                        ChatColor.RED + "Você está prestes a trocar sua classe!",
                        ChatColor.RED + "Todo seu progresso será PERDIDO!",
                        "",
                        ChatColor.GOLD + "Tenha certeza absoluta antes de continuar!"
                )
        );
        menu.setItem(13, warningItem);

        // Preencher com vidro para decoração
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, glass);
            }
        }

        player.openInventory(menu);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Caminho para o arquivo de dados do jogador
        File playerDataFile = new File(plugin.getDataFolder(), "playerdata/" + playerId + ".yml");

        // Verificar se o arquivo existe
        if (!playerDataFile.exists()) {
            // Atrasar um pouco a abertura do menu para garantir que o jogador já entrou completamente
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openClassSelectionMenu(player, true);
                player.sendMessage(ChatColor.GOLD + "Bem-vindo! Por favor, escolha uma classe para começar a jogar.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }, 1200L); // 60 segundo depois
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Menu de seleção de classe
        if (title.equals(ChatColor.DARK_PURPLE + "Escolha sua Classe")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR ||
                    event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }

            String className = null;

            switch (event.getSlot()) {
                case 10: // Minerador
                    className = "Minerador";
                    break;
                case 12: // Caçador
                    className = "Cacador";
                    break;
                case 14: // Pescador
                    className = "Pescador";
                    break;
                case 16: // Ferreiro
                    className = "Ferreiro";
                    break;
                default:
                    return;
            }

            // Definir classe do jogador
            if (className != null) {
                plugin.setPlayerClass(player.getUniqueId(), className);

                // Cancelar a tarefa de forçar escolha
                if (forcedSelectionTasks.containsKey(player.getUniqueId())) {
                    forcedSelectionTasks.get(player.getUniqueId()).cancel();
                    forcedSelectionTasks.remove(player.getUniqueId());
                }

                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Você escolheu a classe " + ChatColor.GOLD + className + ChatColor.GREEN + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // Atualizar scoreboard
                plugin.updateScoreboard(player);
            }
        }

        // Menu de confirmação de troca de classe
        else if (title.equals(ChatColor.RED + "Trocar de Classe?")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR ||
                    event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
                return;
            }

            switch (event.getSlot()) {
                case 11: // Cancelar
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "Operação cancelada.");
                    break;
                case 15: // Confirmar (ir para próxima tela)
                    openFinalTrocaClasseConfirmationMenu(player);
                    break;
            }
        }

        // Menu de confirmação final para troca de classe
        else if (title.equals(ChatColor.DARK_RED + "CONFIRMAÇÃO FINAL")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR ||
                    event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }

            switch (event.getSlot()) {
                case 11: // Cancelar
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "Operação cancelada.");
                    break;
                case 15: // Confirmar troca de classe
                    // Obter dados atuais do jogador
                    UUID playerId = player.getUniqueId();
                    PlayerClassData data = plugin.getPlayerClasses().get(playerId);
                    String currentClass = data.getClassName();

                    // Definir cooldown na PlayerClassData
                    data.setResetCooldownEnd(System.currentTimeMillis() + 86400000); // 24 horas

                    // Salvar dados antes de resetar
                    plugin.savePlayerData(playerId);

                    // Abrir menu de seleção para nova classe
                    openClassSelectionMenu(player, true);

                    player.sendMessage(ChatColor.GOLD + "==================================");
                    player.sendMessage(ChatColor.YELLOW + "Você abandonou a classe " + ChatColor.RED + currentClass);
                    player.sendMessage(ChatColor.YELLOW + "Todo seu progresso foi perdido!");
                    player.sendMessage(ChatColor.YELLOW + "Escolha uma nova classe.");
                    player.sendMessage(ChatColor.RED + "Você não poderá trocar novamente por 24 horas.");
                    player.sendMessage(ChatColor.GOLD + "==================================");

                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.0f);
                    break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // Verificar se o jogador está tentando fechar um menu de seleção forçada
        if (forcedSelectionTasks.containsKey(player.getUniqueId()) &&
                event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Escolha sua Classe")) {

            // Reabrir o menu na próxima tick para evitar problemas de concorrência
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openClassSelectionMenu(player, true);
                player.sendMessage(ChatColor.RED + "Você precisa escolher uma classe para jogar!");
            }, 1L);
        }
    }

    /**
     * Formata o tempo em milissegundos para uma string legível
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return hours + " horas, " + (minutes % 60) + " minutos e " + (seconds % 60) + " segundos";
    }
}