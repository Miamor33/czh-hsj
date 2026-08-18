package com.couple.app.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 为已有 H2 文件库补建 qa_reply（schema.sql 的 IF NOT EXISTS 在部分场景可能未执行）。
 */
@Component
@Order(50)
public class QaReplyTableMigration implements ApplicationRunner {
    private final DataSource dataSource;

    public QaReplyTableMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS qa_reply (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        question_id BIGINT NOT NULL,
                        partner_id BIGINT NOT NULL,
                        content VARCHAR(2000) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
    }
}
