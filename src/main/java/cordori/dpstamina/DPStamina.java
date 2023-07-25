package cordori.dpstamina;

import cordori.dpstamina.command.MainCommand;
import cordori.dpstamina.file.ConfigManager;
import cordori.dpstamina.file.SQLManager;
import cordori.dpstamina.listener.DPSEvent;
import cordori.dpstamina.hook.PAPIHook;
import cordori.dpstamina.task.SQLScheduler;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.task.StaminaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class DPStamina extends JavaPlugin {
    private static DPStamina Instance;
    public static SQLManager sql;
    public static boolean MySQL = false;
    public static DPStamina getInstance() {
        return Instance;
    }


    @Override
    public void onEnable() {
        checkPlugins();
        Instance = this;
        createFile();
        ConfigManager.reloadMyConfig();
        if(getConfig().getString("storage").equalsIgnoreCase("MySQL")) MySQL = true;
        loadSQL();
        Bukkit.getPluginManager().registerEvents(new DPSEvent(), this);
        Bukkit.getPluginCommand("dps").setExecutor(new MainCommand());
        getLogger().info("§e[地牢体力]§b插件加载完成！");
    }

    @Override
    public void onDisable() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            // 插件卸载时把 PlayerData 的玩家数据同步到数据库中
            UUID uuid = player.getUniqueId();
            double stamina = PlayerData.dataHashMap.get(uuid).getStamina();
            String staminaGroup = PlayerData.dataHashMap.get(uuid).getStaminaGroup();

            sql.updateAll(String.valueOf(uuid), stamina, staminaGroup);
        }

        Bukkit.getScheduler().cancelTasks(this);

        // 关闭sql连接
        if(sql != null) sql.disconnect();

        getLogger().info( "§e[地牢体力]§c插件已卸载！感谢你的使用~");
    }

    private void checkPlugins() {
        if(Bukkit.getPluginManager().getPlugin("DungeonPlus") != null) {
            getLogger().info("§e[地牢体力]§b已找到DungeonPlus插件，插件正在加载......");
        } else {
            Bukkit.getPluginManager().disablePlugin(this);
            getLogger().severe("[地牢体力]未找到DungeonPlus插件，插件加载失败！");
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIHook(this).register();
            getLogger().info("§e[地牢体力]§b已找到PlaceholderAPI插件，可使用体力变量！");
        } else {
            getLogger().warning("[地牢体力]未找到PlaceholderAPI插件，无法使用体力变量！");
        }
    }

    private void createFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
            saveDefaultConfig();
        }
    }

    private void loadSQL() {
        String host = getConfig().getString("MySQL.host");
        String port = getConfig().getString("MySQL.port");
        String username = getConfig().getString("MySQL.username");
        String password = getConfig().getString("MySQL.password");
        String fileName = getConfig().getString("MySQL.fileName");
        String tableName = getConfig().getString("MySQL.tableName").toLowerCase();
        String driver;
        driver = getConfig().getString("MySQL.driver");
        String jdbc = getConfig().getString("MySQL.jdbc");
        String sqlString;

        if(MySQL) {
            sqlString = "jdbc:mysql://" + host + ":" + port + "/" + fileName + jdbc;
        } else {
            driver = "org.sqlite.JDBC";
            sqlString = "jdbc:sqlite:" + getDataFolder().toPath().resolve("database.db");
        }

        sql = new SQLManager(sqlString, username, password, driver, tableName);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            sql.createTable();
            for(Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                List<Object> objectsList = DPStamina.sql.getList(String.valueOf(uuid));
                // 如果数据不存在，插入默认数据
                if(objectsList == null) {
                    double stamina = getConfig().getDouble("group.default.limit");
                    PlayerData playerData = new PlayerData("default", stamina);
                    PlayerData.dataHashMap.put(uuid, playerData);
                    DPStamina.sql.insert(String.valueOf(uuid));
                }
            }
            int saveTime = getConfig().getInt("saveTime");
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, new StaminaScheduler(), 0L, 20L * 60L * ConfigManager.minutes);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, new SQLScheduler(), 0L, 20L * 60L * saveTime);
            if(ConfigManager.refresh) {
                Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::refreshOnline, 0L, 20L * 60L);
            }
        });

    }

    private void refreshOnline() {
        LocalTime currentTime = LocalTime.now();

        if (currentTime.isAfter(LocalTime.parse(ConfigManager.refreshTime))) {

            for (Player player : Bukkit.getOnlinePlayers()) {
                sql.insertDate(player);
            }

        }
    }

}
