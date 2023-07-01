package cordori.dpstamina.file;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.utils.LogInfo;
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
    public static boolean refresh;
    public static String refreshTime;
    public static int minutes;
    public static List<String> groupKey = new ArrayList<>();
    public static HashMap<String, String> messagesHashMap = new HashMap<>();
    public static HashMap<String, String> mapCost = new HashMap<>();
    public static HashMap<String, String> ticketNamesMap = new HashMap<>();
    public static HashMap<String, String> mapNamesMap = new HashMap<>();
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
        refresh = dps.getConfig().getBoolean("refresh");
        refreshTime = dps.getConfig().getString("refreshTime");
        regionRecover = dps.getConfig().getBoolean("regionRecover");
        minutes = dps.getConfig().getInt("minutes");
        defaultTicket = dps.getConfig().getString("ticket.default").replaceAll("&","§");
        particularTicket = dps.getConfig().getString("ticket.particular").replaceAll("&","§");
        File file = new File(dps.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadGroup(dps.getConfig());
        loadCost(dps.getConfig());
        loadTicket(dps.getConfig());
        loadRegion(dps.getConfig());
        loadMessages(dps.getConfig());
        loadMapNames(dps.getConfig());
        if(debug) {
            long finishTime = System.currentTimeMillis();
            long useTime = finishTime - startTime;
            LogInfo.debug("重载耗时" + useTime + "ms");
        }

    }

    private static void loadMapNames(FileConfiguration config) {
        mapNamesMap.clear();
        Set<String> mapNames = config.getConfigurationSection("mapNames").getKeys(false);
        for(String mapName : mapNames) {
            String name = config.getString("mapNames." + mapName, mapName).replaceAll("&", "§");
            mapNamesMap.put(mapName, name);
            if(debug) {
                LogInfo.debug("--------------------------");
                LogInfo.debug("地图为：" + mapName);
                LogInfo.debug("地图名为：" + name);
                LogInfo.debug("--------------------------");
            }
        }
    }

    private static void loadRegion(FileConfiguration config) {
        Region.regionHashMap.clear();

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

    private static void loadTicket(FileConfiguration config) {
        ticketNamesMap.clear();

        Set<String> mapNames = config.getConfigurationSection("ticket.particular").getKeys(false);
        for(String mapName : mapNames) {
            String ticketName = config.getString("ticket.particular." + mapName).replaceAll("&", "§");
            ticketNamesMap.put(mapName, ticketName);
            if(debug) {
                LogInfo.debug("--------------------------");
                LogInfo.debug("地图名为：" + mapName);
                LogInfo.debug("门票名为：" + ticketName);
                LogInfo.debug("--------------------------");
            }
        }
    }

    private static void loadCost(FileConfiguration config) {
        mapCost.clear();

        Set<String> costSet = config.getConfigurationSection("cost").getKeys(false);
        for(String mapName : costSet) {
            String cost = config.getString("cost." + mapName);
            if(cost.startsWith("PAPI:")) {
                cost = cost.substring(5);
            }
            mapCost.put(mapName, cost);

            if(debug) {
                LogInfo.debug("-------------------------------");
                LogInfo.debug("地图名: " + mapName);
                LogInfo.debug("体力消耗: " + cost);
                LogInfo.debug("-------------------------------");
            }
        }
    }

    private static void loadGroup(FileConfiguration config) {
        groupKey.clear();
        StaminaGroup.groupHashMap.clear();

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
                LogInfo.debug("-------------------------------");
                LogInfo.debug("体力组名称: " + groupName);
                LogInfo.debug("体力上限: " + limit);
                LogInfo.debug("体力恢复量: " + recover);
                LogInfo.debug("-------------------------------");
            }
        }
    }

    private static void loadMessages(FileConfiguration config) {
        messagesHashMap.clear();

        Set<String> messages = config.getConfigurationSection("messages").getKeys(false);
        for(String key : messages) {
            String message = config.getString("messages." + key).replaceAll("&", "§");
            if(message.equals("")) continue;
            messagesHashMap.put(key, message);

            if(debug) {
                LogInfo.debug(key + ": " + message);
            }
        }
    }
}
