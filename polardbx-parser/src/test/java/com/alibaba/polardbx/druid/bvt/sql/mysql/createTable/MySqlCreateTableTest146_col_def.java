/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.druid.bvt.sql.mysql.createTable;

import com.alibaba.polardbx.druid.sql.ast.SQLStatement;
import com.alibaba.polardbx.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.polardbx.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import junit.framework.TestCase;

import java.util.List;

/**
 * @version 1.0
 */
public class MySqlCreateTableTest146_col_def extends TestCase {

    public void test_0() throws Exception {

        String sql = "create table tb_dxdd (" +
            "`a` varchar(10) not null default 'val' comment 'hehe' collate utf8_unicode_ci column_format default storage disk references tb_ref (a) match full on delete cascade on update cascade"
            +
            ");";

        MySqlStatementParser parser = new MySqlStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        MySqlCreateTableStatement stmt = (MySqlCreateTableStatement) statementList.get(0);

        assertEquals(1, statementList.size());

        /*
        assertEquals("CREATE TABLE tb_dxdd (\n" +
                "\t`a` varchar(10) NOT NULL DEFAULT 'val' COMMENT 'hehe' COLLATE utf8_unicode_ci COLUMN_FORMAT DEFAULT STORAGE disk REFERENCES tb_ref (a) MATCH FULL ON DELETE CASCADE ON UPDATE CASCADE\n" +
                ");", stmt.toString());

        assertEquals("create table tb_dxdd (\n" +
                "\t`a` varchar(10) not null default 'val' comment 'hehe' collate utf8_unicode_ci column_format default storage disk references tb_ref (a) match full on delete cascade on update cascade\n" +
                ");", stmt.toLowerCaseString());
        */
        // Output order bad.
        assertEquals("CREATE TABLE tb_dxdd (\n" +
            "\t`a` varchar(10) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'val' STORAGE disk COLUMN_FORMAT DEFAULT COMMENT 'hehe' REFERENCES tb_ref (a) MATCH FULL ON DELETE CASCADE ON UPDATE CASCADE\n"
            +
            ");", stmt.toString());

        assertEquals("create table tb_dxdd (\n" +
            "\t`a` varchar(10) collate utf8_unicode_ci not null default 'val' storage disk column_format default comment 'hehe' references tb_ref (a) match full on delete cascade on update cascade\n"
            +
            ");", stmt.toLowerCaseString());
    }

    public void test_1() throws Exception {
        String sql = "create table tb_xx (a int generated always as (1) virtual not null comment 'xxx');";

        MySqlStatementParser parser = new MySqlStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        MySqlCreateTableStatement stmt = (MySqlCreateTableStatement) statementList.get(0);

        assertEquals(1, statementList.size());

        assertEquals("CREATE TABLE tb_xx (\n" +
            "\ta int GENERATED ALWAYS AS (1) VIRTUAL NOT NULL COMMENT 'xxx'\n" +
            ");", stmt.toString());

        assertEquals("create table tb_xx (\n" +
            "\ta int generated always as (1) virtual not null comment 'xxx'\n" +
            ");", stmt.toLowerCaseString());
    }

    public void test_2() throws Exception {
        String sql =
            "create table tb_ssx (a varchar(10) collate utf8_general_ci as ('val') stored not null primary key comment 'hh' references tb_ref (a));";

        MySqlStatementParser parser = new MySqlStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        MySqlCreateTableStatement stmt = (MySqlCreateTableStatement) statementList.get(0);

        assertEquals(1, statementList.size());

        /*
        assertEquals("CREATE TABLE tb_ssx (\n" +
                "\ta varchar(10) COLLATE utf8_general_ci AS ('val') SORTED NOT NULL PRIMARY KEY COMMENT 'hh' REFERENCES tb_ref (a)\n" +
                ");", stmt.toString());

        assertEquals("create table tb_ssx (\n" +
                "\ta varchar(10) collate utf8_general_ci as ('val') sorted not null primary key comment 'hh' references tb_ref (a)\n" +
                ");", stmt.toLowerCaseString());
        */
        // Output order bad.
        assertEquals("CREATE TABLE tb_ssx (\n" +
            "\ta varchar(10) COLLATE utf8_general_ci GENERATED ALWAYS AS ('val') STORED NOT NULL PRIMARY KEY COMMENT 'hh' REFERENCES tb_ref (a)\n"
            +
            ");", stmt.toString());

        assertEquals("create table tb_ssx (\n" +
            "\ta varchar(10) collate utf8_general_ci generated always as ('val') stored not null primary key comment 'hh' references tb_ref (a)\n"
            +
            ");", stmt.toLowerCaseString());
    }

}
