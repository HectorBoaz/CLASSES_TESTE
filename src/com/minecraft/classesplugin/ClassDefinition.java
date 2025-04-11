package com.minecraft.classesplugin;

import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.*;

public class ClassDefinition {
    private String name;
    private Map<Integer, Integer> levelRequirements;
    private Map<Integer, List<String>> levelPermissions;
    private List<Quest> quests;
    private Set<Material> allowedTools;
    private Map<Integer, Particle> particleEffects;
    
    public ClassDefinition(String name) {
        this.name = name;
        this.levelRequirements = new HashMap<>();
        this.levelPermissions = new HashMap<>();
        this.quests = new ArrayList<>();
        this.allowedTools = new HashSet<>();
        this.particleEffects = new HashMap<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void addLevelRequirement(int level, int xpRequired) {
        levelRequirements.put(level, xpRequired);
    }
    
    public int getLevelRequirement(int level) {
        return levelRequirements.getOrDefault(level, -1);
    }
    
    public void addLevelPermission(int level, String permission) {
        List<String> perms = levelPermissions.getOrDefault(level, new ArrayList<>());
        perms.add(permission);
        levelPermissions.put(level, perms);
    }
    
    public List<String> getLevelPermissions(int level) {
        return levelPermissions.getOrDefault(level, new ArrayList<>());
    }
    
    public void addQuest(Quest quest) {
        quests.add(quest);
    }
    
    public List<Quest> getQuests() {
        return quests;
    }
    
    public void addAllowedTool(Material tool) {
        allowedTools.add(tool);
    }
    
    public boolean isAllowedTool(Material tool) {
        return allowedTools.contains(tool);
    }
    
    public void setParticleEffect(int level, Particle effect) {
        particleEffects.put(level, effect);
    }
    
    public Particle getParticleEffect(int level) {
        // Encontrar o efeito para o nível mais alto que o jogador possui
        Particle result = null;
        for (int i = 1; i <= level; i++) {
            if (particleEffects.containsKey(i)) {
                result = particleEffects.get(i);
            }
        }
        return result;
    }
}