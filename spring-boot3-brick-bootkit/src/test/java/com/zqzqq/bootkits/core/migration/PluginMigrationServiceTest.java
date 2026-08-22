/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
