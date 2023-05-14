package cordori.dpstamina.file;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.utils.Region;
import cordori.dpstamina.utils.StaminaGroup;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigManager {
    public static boolean debug;
    public static String prefix;
    public static double defaultCost;
    public static boolean offline;
    public static int minutes;
    public static List<String> groupKey = new ArrayList<>();
    public static HashMap<String, String> messagesHashMap = new HashMap<>();
    public static HashMap<String, String> mapCost = new HashMap<>();
    public static boolean ticket = false;
    public static String defaultTicket;
    public static String particularTicket;
    public static boolean regionRecover;

    public static void reloadMyConfig() {
        long startTime = System.currentTimeMillis();
        DPStamina dps = DPStamina.getInstance();
        dps.reloadConfig();
        debug = dps.getConfig().getBoolean("debug");
        prefix = dps.getConfig().getString("prefix").replaceAll("&","§");
        defaultCost = dps.getConfig().getDouble("cost.default");
        offline = dps.getConfig().getBoolean("offline");
        regionRecover = dps.getConfig().getBoolean("regionRecover");
        minutes = dps.getConfig().getInt("minutes");
        ticket = dps.getConfig().getBoolean("ticket.enable");
        defaultTicket = dps.getConfig().getString("ticket.default").replaceAll("&","§");
        particularTicket = dps.getConfig().getString("ticket.particular").replaceAll("&","§");
        File file = new File(dps.getDataFolder(), "config.yml");
        loadGroup(file);
        loadCost(file);
        loadRegion(file);
        loadMessages(file);

        if(debug) {
            long finishTime = System.currentTimeMillis();
            long useTime = finishTime - startTime;
            System.out.println("重载耗时" + useTime + "ms");
        }

    }

    private static void loadRegion(File file) {
        Region.regionHashMap.clear();

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> regions = config.getStringList("regions");

        for(String region : regions) {

            String[] reg = region.split(";");

            // 获取区域名称和两个坐标点的坐标值
            String worldName = reg[0];
            String[] pos1 = reg[1].split(",");
            String[] pos2 = reg[2].split(",");

            Region.regionHashMap.put(worldName, new Region(pos1, pos2));

        }


    }

    private static void loadCost(File file) {
        mapCost.clear();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> costSet = config.getConfigurationSection("cost").getKeys(false);
        for(String mapName : costSet) {
            String cost = config.getString("cost." + mapName);
            if(cost.startsWith("PAPI:")) {
                cost = cost.substring(5);
            }
            mapCost.put(mapName, cost);

            if(debug) {
                System.out.println("-------------------------------");
                System.out.println("地图名: " + mapName);
                System.out.println("体力消耗: " + cost);
                System.out.println("-------------------------------");
            }
        }
    }

    private static void loadGroup(File file) {
        groupKey.clear();
        StaminaGroup.groupHashMap.clear();

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

    private static void loadMessages(File file) {
        messagesHashMap.clear();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> messages = config.getConfigurationSection("messages").getKeys(false);
        for(String key : messages) {
            String message = config.getString("messages." + key).replaceAll("&", "§");
            if(message.equals("")) continue;
            messagesHashMap.put(key, message);

            if(debug) {
                System.out.println("处理后的信息: " + message);
            }
        }
    }
}
