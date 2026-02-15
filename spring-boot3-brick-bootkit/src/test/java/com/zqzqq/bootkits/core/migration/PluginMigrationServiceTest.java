package com.zqzqq.bootkits.core.migration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginMigrationServiceTest {

    @Test
    void splitSqlStatements_shouldIgnoreCommentsAndKeepQuotedSemicolon() {
        String sql = "-- line comment\n"
                + "CREATE TABLE t1(id INT);\n"
                + "INSERT INTO t1(name) VALUES('a;b');\n"
                + "/* block ; comment */\n"
                + "UPDATE t1 SET name=\"x;y\" WHERE id=1;";

        List<String> statements = PluginMigrationService.splitSqlStatements(sql);

        assertThat(statements).hasSize(3);
        assertThat(statements.get(0)).startsWith("CREATE TABLE t1");
        assertThat(statements.get(1)).contains("'a;b'");
        assertThat(statements.get(2)).contains("\"x;y\"");
    }
}
