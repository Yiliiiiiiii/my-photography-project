package com.yiliiii.project.my_photography_project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Component
@Profile("win")
public class DesktopDataMigration {

    private static final Logger logger = LoggerFactory.getLogger(DesktopDataMigration.class);

    private static final List<String> TABLES = List.of(
            "roles",
            "users",
            "tag",
            "photo",
            "album",
            "daily_summary",
            "comment",
            "notification",
            "users_roles",
            "photo_likes",
            "photo_tags",
            "album_photos");

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate targetJdbc;
    private final DataSource targetDataSource;
    private final String webConfigPath;

    public DesktopDataMigration(
            JdbcTemplate targetJdbc,
            DataSource targetDataSource,
            @Value("${myapp.web-config-path:}") String webConfigPath) {
        this.targetJdbc = targetJdbc;
        this.targetDataSource = targetDataSource;
        this.webConfigPath = webConfigPath;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateIfNeeded() {
        if (webConfigPath == null || webConfigPath.isBlank()) {
            status("No Web application.properties found; using fresh desktop data.");
            return;
        }

        Path configPath = Path.of(webConfigPath);
        if (!Files.exists(configPath)) {
            status("Web config path does not exist: " + configPath);
            return;
        }

        if (hasDesktopBusinessData()) {
            status("Desktop database already has business data; migration skipped.");
            return;
        }

        Properties webProperties;
        try {
            webProperties = loadProperties(configPath);
        } catch (IOException e) {
            logger.warn("[desktop migration] Could not read Web config: {}", configPath, e);
            status("Could not read Web config: " + e.getMessage());
            return;
        }

        String sourceUrl = webProperties.getProperty("spring.datasource.url", "").trim();
        String sourceUsername = webProperties.getProperty("spring.datasource.username", "").trim();
        String sourcePassword = webProperties.getProperty("spring.datasource.password", "");

        if (sourceUrl.isBlank() || sourceUrl.startsWith("jdbc:h2:")) {
            status("Web MySQL datasource not found in " + configPath + "; migration skipped.");
            return;
        }

        String migrationSourceUrl = withConnectionTimeouts(sourceUrl);
        status("Starting migration from: " + sourceUrl);

        try (Connection source = DriverManager.getConnection(migrationSourceUrl, sourceUsername, sourcePassword);
                Connection target = targetDataSource.getConnection()) {
            status("Connected to source MySQL. Copying tables into desktop H2.");
            migrateTables(source, target);
            status("Migration completed.");
        } catch (Exception e) {
            logger.warn("[desktop migration] Migration failed. Desktop app will continue with current H2 data.", e);
            status("Migration failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String withConnectionTimeouts(String sourceUrl) {
        String separator = sourceUrl.contains("?") ? "&" : "?";

        if (sourceUrl.contains("connectTimeout=")) {
            return sourceUrl;
        }

        return sourceUrl + separator + "connectTimeout=5000&socketTimeout=20000";
    }

    private void status(String message) {
        logger.info("[desktop migration] {}", message);

        if (webConfigPath == null || webConfigPath.isBlank()) {
            return;
        }

        try {
            Path logPath = Path.of(webConfigPath).toAbsolutePath().getParent().resolve("migration.log");
            String line = java.time.LocalDateTime.now() + " " + message + System.lineSeparator();
            Files.writeString(logPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Logging to file is best effort; application logs still contain the status.
        }
    }

    private boolean hasDesktopBusinessData() {
        return countIfTableExists("users") > 0
                || countIfTableExists("photo") > 0
                || countIfTableExists("album") > 0
                || countIfTableExists("comment") > 0;
    }

    private int countIfTableExists(String table) {
        try {
            return targetJdbc.queryForObject("select count(*) from " + table, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();

        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }

            int separator = findSeparator(trimmed);
            if (separator <= 0) {
                continue;
            }

            String key = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            properties.setProperty(key, value);
        }

        return properties;
    }

    private int findSeparator(String line) {
        int equals = line.indexOf('=');
        int colon = line.indexOf(':');

        if (equals < 0) {
            return colon;
        }

        if (colon < 0) {
            return equals;
        }

        return Math.min(equals, colon);
    }

    private void migrateTables(Connection source, Connection target) throws SQLException {
        Set<String> sourceTables = getTableNames(source);
        Set<String> targetTables = getTableNames(target);

        try (Statement statement = target.createStatement()) {
            statement.execute("set referential_integrity false");
        }

        try {
            clearTargetTables(target, targetTables);

            for (String table : TABLES) {
                if (!sourceTables.contains(table) || !targetTables.contains(table)) {
                    status("Table " + table + " not present in both databases; skipped.");
                    continue;
                }

                int copied = copyTable(source, target, table);
                status("Copied " + copied + " rows from " + table + ".");
            }

            restartIdentityColumns(target, targetTables);
        } finally {
            try (Statement statement = target.createStatement()) {
                statement.execute("set referential_integrity true");
            }
        }
    }

    private Set<String> getTableNames(Connection connection) throws SQLException {
        Set<String> names = new HashSet<>();
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, "%", new String[] { "TABLE" })) {
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                if (tableName != null) {
                    names.add(tableName.toLowerCase(Locale.ROOT));
                }
            }
        }

        return names;
    }

    private void clearTargetTables(Connection target, Set<String> targetTables) throws SQLException {
        List<String> reverseTables = new ArrayList<>(TABLES);
        java.util.Collections.reverse(reverseTables);

        try (Statement statement = target.createStatement()) {
            for (String table : reverseTables) {
                if (targetTables.contains(table)) {
                    statement.executeUpdate("delete from " + table);
                }
            }
        }
    }

    private int copyTable(Connection source, Connection target, String table) throws SQLException {
        Map<String, String> targetColumns = getColumns(target, table);
        int copied = 0;

        try (Statement select = source.createStatement();
                ResultSet rows = select.executeQuery("select * from " + table)) {
            ResultSetMetaData rowMeta = rows.getMetaData();
            List<String> sourceColumns = new ArrayList<>();

            for (int i = 1; i <= rowMeta.getColumnCount(); i++) {
                String column = rowMeta.getColumnLabel(i);
                if (column != null && targetColumns.containsKey(column.toLowerCase(Locale.ROOT))) {
                    sourceColumns.add(column);
                }
            }

            if (sourceColumns.isEmpty()) {
                return 0;
            }

            String insertSql = buildInsertSql(table, sourceColumns, targetColumns);

            try (PreparedStatement insert = target.prepareStatement(insertSql)) {
                while (rows.next()) {
                    for (int i = 0; i < sourceColumns.size(); i++) {
                        insert.setObject(i + 1, rows.getObject(sourceColumns.get(i)));
                    }
                    insert.addBatch();
                    copied++;

                    if (copied % BATCH_SIZE == 0) {
                        insert.executeBatch();
                    }
                }
                insert.executeBatch();
            }
        }

        return copied;
    }

    private Map<String, String> getColumns(Connection connection, String table) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, table, "%")) {
            while (resultSet.next()) {
                String column = resultSet.getString("COLUMN_NAME");
                if (column != null) {
                    columns.put(column.toLowerCase(Locale.ROOT), column.toLowerCase(Locale.ROOT));
                }
            }
        }

        if (columns.isEmpty()) {
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, table.toUpperCase(Locale.ROOT),
                    "%")) {
                while (resultSet.next()) {
                    String column = resultSet.getString("COLUMN_NAME");
                    if (column != null) {
                        columns.put(column.toLowerCase(Locale.ROOT), column.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }

        return columns;
    }

    private String buildInsertSql(String table, List<String> sourceColumns, Map<String, String> targetColumns) {
        List<String> insertColumns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (String sourceColumn : sourceColumns) {
            insertColumns.add(targetColumns.get(sourceColumn.toLowerCase(Locale.ROOT)));
            placeholders.add("?");
        }

        return "insert into " + table
                + " (" + String.join(", ", insertColumns) + ") values ("
                + String.join(", ", placeholders) + ")";
    }

    private void restartIdentityColumns(Connection target, Set<String> targetTables) {
        Map<String, String> identityColumns = new HashMap<>();
        identityColumns.put("roles", "id");
        identityColumns.put("users", "id");
        identityColumns.put("tag", "id");
        identityColumns.put("photo", "id");
        identityColumns.put("album", "id");
        identityColumns.put("daily_summary", "id");
        identityColumns.put("comment", "id");
        identityColumns.put("notification", "id");

        for (Map.Entry<String, String> entry : identityColumns.entrySet()) {
            String table = entry.getKey();
            String column = entry.getValue();

            if (!targetTables.contains(table)) {
                continue;
            }

            try {
                Integer maxId = targetJdbc.queryForObject("select max(" + column + ") from " + table, Integer.class);
                int nextId = maxId == null ? 1 : maxId + 1;
                targetJdbc.execute("alter table " + table + " alter column " + column + " restart with " + nextId);
            } catch (Exception e) {
                logger.debug("[desktop migration] Could not restart identity for {}.{}.", table, column, e);
            }
        }
    }
}
