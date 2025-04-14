package com.minecraft.classesplugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class    PlayerClassData {
    private String className;
    private int level;
    private int xp;
    private Set<String> completedQuests;
    
    public PlayerClassData(String className, int level, int xp) {
        this.className = className;
        this.level = level;
        this.xp = xp;
        this.completedQuests = new HashSet<>();
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getXp() {
        return xp;
    }
    
    public void setXp(int xp) {
        this.xp = xp;
    }
    
    public void addXp(int amount) {
        this.xp += amount;
    }
    
    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId);
    }
    
    public void completeQuest(String questId) {
        completedQuests.add(questId);
    }
    
    public Set<String> getCompletedQuests() {
        return completedQuests;
    }

    }