package cordori.dpstamina.file;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.StaminaGroup;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.dbcp2.BasicDataSource;
import org.bukkit.entity.Player;

public class SQLManager {
    private final String tableName;
    @Getter private final BasicDataSource dataSource = new BasicDataSource();

    @SneakyThrows
    public SQLManager(String url, String username, String password, String driver, String tableName) {
        this.tableName = tableName;
        Class.forName(driver);
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);

        if(DPStamina.MySQL) {
            dataSource.setUsername(username);
            dataSource.setPassword(password);
        }
    }

    @SneakyThrows
    public Connection getConnection() {
        return  dataSource.getConnection();
    }

    @SneakyThrows
    public void disconnect() {
        getConnection().close();
    }

    @SneakyThrows
    public void createTable() {
        @Cleanup Connection connection = getConnection();
        @Cleanup Statement statement = connection.createStatement();
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid VARCHAR(255) PRIMARY KEY, "
                + "staminaGroup VARCHAR(255), "
                + "stamina DOUBLE, "
                + "offlineTime BIGINT, "
                + "refreshDate DATE)";
        statement.executeUpdate(query);

        // 检查是否需要添加 refreshDate 列
        DatabaseMetaData meta = connection.getMetaData();
        @Cleanup ResultSet rs = meta.getColumns(null, null, tableName, "refreshDate");

        // 检查是否存在 refreshDate 列
        if (!rs.next()) {
            // 列不存在，执行 ALTER TABLE 语句添加 refreshDate 列
            @Cleanup Statement stmt = connection.createStatement();
            String alterQuery = "ALTER TABLE " + tableName + " ADD COLUMN refreshDate DATE";
            stmt.executeUpdate(alterQuery);
        }

    }

    @SneakyThrows
    public List<Object> getList(String uuid) {
        List<Object> objectList = new ArrayList<>();
        @Cleanup Connection conn = getConnection();
        String selectQuery = "SELECT staminaGroup, stamina, offlineTime FROM " + tableName + " WHERE uuid = ?";
        @Cleanup PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
        selectStmt.setString(1, uuid);
        @Cleanup ResultSet rs = selectStmt.executeQuery();
        if (rs.next()) {
            String staminaGroup = rs.getString("staminaGroup");
            double stamina = rs.getDouble("stamina");
            long lastTime = rs.getLong("offlineTime");
            objectList.add(staminaGroup);
            objectList.add(stamina);
            objectList.add(lastTime);
        } else {
            objectList = null;
        }
        return objectList;
    }

    @SneakyThrows
    public void insert(String uuid) {
        @Cleanup Connection conn = getConnection();

        String selectQuery = "SELECT staminaGroup, stamina, offlineTime FROM " + tableName + " WHERE uuid = ?";
        @Cleanup PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
        selectStmt.setString(1, uuid);
        @Cleanup ResultSet rs = selectStmt.executeQuery();

        if (!rs.next()) {
            String insertQuery = "INSERT INTO " + tableName + " (uuid, staminaGroup, stamina, offlineTime) VALUES (?, ?, ?, ?)";
            @Cleanup PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, uuid);
            insertStmt.setString(2, "default");
            insertStmt.setDouble(3, 100.0);
            insertStmt.setLong(4, System.currentTimeMillis());
            insertStmt.executeUpdate();
        }
    }

    @SneakyThrows
    public void insertDate(Player player) {
        UUID uuid = player.getUniqueId();
        @Cleanup Connection conn = getConnection();
        String selectQuery = "SELECT refreshDate FROM " + tableName + " WHERE uuid = ?";
        @Cleanup PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
        selectStmt.setString(1, uuid.toString());
        @Cleanup ResultSet rs = selectStmt.executeQuery();
        LocalDate currentDate = LocalDate.now();

        // 判断是否存在 refreshDate 记录
        if (rs.next()) {
            LocalDate refreshDate = null;
            Date date = rs.getDate("refreshDate");

            if (date != null) {
                refreshDate = date.toLocalDate();
            }

            // 判断日期是否相同
            if (date == null || !currentDate.equals(refreshDate)) {
                LocalTime currentTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
                LocalTime refreshTime = LocalTime.parse(ConfigManager.refreshTime);
                if(ConfigManager.debug) {
                    System.out.println(currentDate);
                    System.out.println(refreshTime);
                    System.out.println(currentTime.isAfter(refreshTime));
                    System.out.println(currentTime.equals(refreshTime));
                }
                // 当前时间超过刷新时间，更新日期记录并恢复玩家体力至上限，发送消息
                if (currentTime.isAfter(refreshTime) || currentTime.equals(refreshTime)) {
                    String updateQuery = "UPDATE " + tableName + " SET refreshDate = ? WHERE uuid = ?";
                    @Cleanup PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setDate(1, Date.valueOf(currentDate));
                    updateStmt.setString(2, uuid.toString());
                    updateStmt.executeUpdate();

                    String staminaGroup = PlayerData.dataHashMap.get(uuid).getStaminaGroup();
                    double limit = StaminaGroup.groupHashMap.get(staminaGroup).getLimit();
                    PlayerData.dataHashMap.get(uuid).setStamina(limit);
                    if (ConfigManager.messagesHashMap.containsKey("refresh")) {
                        player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("refresh"));
                    }
                }
            }
        }
    }

    @SneakyThrows
    public void updateAll(String uuid, double stamina, String staminaGroup) {
        @Cleanup Connection conn = getConnection();
        String updateQuery = "UPDATE " + tableName + " SET stamina = ?, staminaGroup = ?, offlineTime = ? WHERE uuid = ?";
        @Cleanup PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
        updateStmt.setDouble(1, stamina);
        updateStmt.setString(2, staminaGroup);
        updateStmt.setLong(3, System.currentTimeMillis());
        updateStmt.setString(4, uuid);
        updateStmt.executeUpdate();
    }

}
