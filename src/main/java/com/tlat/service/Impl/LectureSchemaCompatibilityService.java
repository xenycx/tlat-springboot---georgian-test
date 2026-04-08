package com.tlat.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LectureSchemaCompatibilityService {

    private static final Logger log = LoggerFactory.getLogger(LectureSchemaCompatibilityService.class);

    private final JdbcTemplate jdbcTemplate;

    public LectureSchemaCompatibilityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateLegacyLectureColumnsIfNeeded() {
        try {
            String databaseProduct = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                    connection.getMetaData().getDatabaseProductName());

            if (databaseProduct == null || !databaseProduct.toLowerCase().contains("mysql")) {
                log.info("Skipping lecture legacy schema compatibility for database type: {}", databaseProduct);
                return;
            }

            makeColumnNullable("lectures", "date", "date");
            makeColumnNullable("lectures", "start_time", "time");
            makeColumnNullable("lectures", "end_time", "time");
            makeColumnNullable("lectures", "room_id", "bigint");
            makeColumnNullable("lectures", "is_active", "bit(1)");
            makeColumnNullable("lectures", "status", "varchar(255)");

            log.info("Lecture legacy schema compatibility migration completed");
        } catch (DataAccessException ex) {
            log.error("Failed to run lecture legacy schema compatibility migration", ex);
            throw ex;
        }
    }

    private void makeColumnNullable(String tableName, String columnName, String typeDefinition) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );

        if (columnCount == null || columnCount == 0) {
            return;
        }

        String nullable = jdbcTemplate.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                String.class,
                tableName,
                columnName
        );

        if (!"YES".equalsIgnoreCase(nullable)) {
            String sql = "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + typeDefinition + " NULL";
            log.info("Applying schema compatibility: {}", sql);
            jdbcTemplate.execute(sql);
        }
    }
}
