/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.USER_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.SCM_ACCOUNT_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.SCM_ACCOUNT_SIZE;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.USER_UUID_COLUMN_NAME;

public class CreateScmAccountsTableTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createEmpty();

  private final DdlChange createScmAccountsTable = new CreateScmAccountsTable(db.database());

  @Test
  public void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    createScmAccountsTable.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, USER_UUID_COLUMN_NAME, Types.VARCHAR, USER_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, SCM_ACCOUNT_COLUMN_NAME, Types.VARCHAR, SCM_ACCOUNT_SIZE, false);
    db.assertPrimaryKey(TABLE_NAME, "pk_scm_accounts", USER_UUID_COLUMN_NAME, SCM_ACCOUNT_COLUMN_NAME);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    createScmAccountsTable.execute();
    // re-entrant
    createScmAccountsTable.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
