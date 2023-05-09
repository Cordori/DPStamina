package cordori.dpstamina.file;

import cordori.dpstamina.DPStamina;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;

public class SQLManager {
    private final String username;
    private final String password;
    private final String url;
    @Getter private final BasicDataSource dataSource = new BasicDataSource();

    @SneakyThrows
    public SQLManager(String url, String username, String password, String driver) {
        this.username = username;
        this.password = password;
        Class.forName(driver);
        this.url = url;
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
    }

    @SneakyThrows
    public Connection getConnection() {
        return DPStamina.MySQL ? DriverManager.getConnection(url, username, password) : dataSource.getConnection();
    }

    @SneakyThrows
    public void disconnect() {
        getConnection().close();
    }

    @SneakyThrows
    public void createTable() {
        @Cleanup Connection connection = getConnection();
        @Cleanup Statement statement = connection.createStatement();
        String query = "CREATE TABLE IF NOT EXISTS dpstaminadata ("
                + "uuid VARCHAR(255) PRIMARY KEY, "
                + "staminaGroup VARCHAR(255), "
                + "stamina DOUBLE, "
                + "offlineTime BIGINT)";
        statement.executeUpdate(query);
    }

    @SneakyThrows
    public List<Object> getList(String uuid) {
        List<Object> objectList = new ArrayList<>();
        @Cleanup Connection conn = getConnection();
        String selectQuery = "SELECT staminaGroup, stamina, offlineTime FROM dpstaminadata WHERE uuid = ?";
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

        String selectQuery = "SELECT staminaGroup, stamina, offlineTime FROM dpstaminadata WHERE uuid = ?";
        @Cleanup PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
        selectStmt.setString(1, uuid);
        @Cleanup ResultSet rs = selectStmt.executeQuery();

        if (!rs.next()) {
            String insertQuery = "INSERT INTO dpstaminadata (uuid, staminaGroup, stamina, offlineTime) VALUES (?, ?, ?, ?)";
            @Cleanup PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, uuid);
            insertStmt.setString(2, "default");
            insertStmt.setDouble(3, 100.0);
            insertStmt.setLong(4, System.currentTimeMillis());
            insertStmt.executeUpdate();
        }
    }

    @SneakyThrows
    public void updateAll(String uuid, double stamina, String staminaGroup) {
        @Cleanup Connection conn = getConnection();
        String updateQuery = "UPDATE dpstaminadata SET stamina = ?, staminaGroup = ?, offlineTime = ? WHERE uuid = ?";
        @Cleanup PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
        updateStmt.setDouble(1, stamina);
        updateStmt.setString(2, staminaGroup);
        updateStmt.setLong(3, System.currentTimeMillis());
        updateStmt.setString(4, uuid);
        updateStmt.executeUpdate();
    }

}
