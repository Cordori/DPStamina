package cordori.dpstamina.file;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.utils.StaminaGroup;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigManager {
    public static boolean debug;
    public static String prefix;
    public static double cost;
    public static boolean offline;
    public static int minutes;
    public static List<String> groupKey = new ArrayList<>();
    public static HashMap<String, String> messagesHashMap = new HashMap<>();

    public static void reloadMyConfig() {
        DPStamina dps = DPStamina.getInstance();
        dps.reloadConfig();

        debug = dps.getConfig().getBoolean("debug");
        prefix = dps.getConfig().getString("prefix").replaceAll("&","§");
        cost = dps.getConfig().getDouble("cost");
        offline = dps.getConfig().getBoolean("offline");
        minutes = dps.getConfig().getInt("minutes");

        loadGroup(dps);
        loadMessages(dps);
    }

    private static void loadGroup(DPStamina dps) {
        groupKey.clear();
        StaminaGroup.groupHashMap.clear();
        File file = new File(dps.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> groupNames = config.getConfigurationSection("group").getKeys(false);
        for(String groupName : groupNames) {
            double limit = config.getDouble("group." + groupName + ".limit", 100);
            String recover = config.getString("group." + groupName + ".recover", "1");
            if(recover.startsWith("PAPI:")) {
                recover = recover.substring(5);
            }
            groupKey.add(groupName);
            StaminaGroup.groupHashMap.put(groupName, new StaminaGroup(limit, recover));

            if(debug) {
                System.out.println("-------------------------------");
                System.out.println("体力组名称: " + groupName);
                System.out.println("体力上限: " + limit);
                System.out.println("体力恢复量: " + recover);
                System.out.println("-------------------------------");
            }
        }
    }

    private static void loadMessages(DPStamina dps) {
        messagesHashMap.clear();
        File file = new File(dps.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> messages = config.getConfigurationSection("messages").getKeys(false);
        for(String key : messages) {
            String message = config.getString("messages." + key).replaceAll("&", "§");
            messagesHashMap.put(key, message);

            if(debug) {
                System.out.println("处理后的信息: " + message);
            }
        }
    }
}
