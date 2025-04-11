package com.minecraft.classesplugin;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class Quest {
    private String id;
    private String description;
    private int targetAmount;
    private QuestType type;
    private Object target;  // Pode ser Material ou EntityType
    private int rewardXp;
    
    public Quest(String id, String description, int targetAmount, QuestType type, Object target, int rewardXp) {
        this.id = id;
        this.description = description;
        this.targetAmount = targetAmount;
        this.type = type;
        this.target = target;
        this.rewardXp = rewardXp;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getTargetAmount() {
        return targetAmount;
    }
    
    public QuestType getType() {
        return type;
    }
    
    public Object getTarget() {
        return target;
    }
    
    public Material getTargetMaterial() {
        if (target instanceof Material) {
            return (Material) target;
        }
        return null;
    }
    
    public EntityType getEntityTarget() {
        if (target instanceof EntityType) {
            return (EntityType) target;
        }
        return null;
    }
    
    public int getRewardXp() {
        return rewardXp;
    }
}