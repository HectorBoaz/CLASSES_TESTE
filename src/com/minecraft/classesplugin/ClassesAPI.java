package com.minecraft.classesplugin;

import java.util.Map;
import java.util.UUID;

public class ClassesAPI {
    private static Main instance;

    public static void init(Main plugin) {
        instance = plugin;
    }

    public static Map<UUID, PlayerClassData> getPlayerClasses() {
        return instance.getPlayerClasses();
    }

    public static String getPlayerClassName(UUID playerId) {
        PlayerClassData data = getPlayerClasses().get(playerId);
        return data != null ? data.getClassName() : null;
    }

    public static int getPlayerLevel(UUID playerId) {
        PlayerClassData data = getPlayerClasses().get(playerId);
        return data != null ? data.getLevel() : 0;
    }
}