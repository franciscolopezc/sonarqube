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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;

public class RemoveUserActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db));
  private final WsActionTester ws = new WsActionTester(new RemoveUserAction(dbClient, wsSupport));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("remove_user");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("login", "gateName");
  }

  @Test
  public void remove_user() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGate, user)).isFalse();
  }

  @Test
  public void does_nothing_when_user_cannot_edit_gate() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();

    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGate, user)).isFalse();

    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGate, user)).isFalse();
  }

  @Test
  public void qg_administrators_can_remove_user() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGate, user)).isFalse();
  }

  @Test
  public void qg_editors_can_remove_user() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    UserDto userAllowedToEditGate = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, userAllowedToEditGate);
    userSession.logIn(userAllowedToEditGate);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGate, user)).isFalse();
  }

  @Test
  public void fail_when_user_does_not_exist() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    final TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_LOGIN, "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User with login 'unknown' is not found");
  }

  @Test
  public void fail_when_qgate_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    final TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, "unknown")
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No quality gate has been found for name unknown");
  }

  @Test
  public void fail_when_qg_is_built_in() {
    UserDto user = db.users().insertUser();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    final TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));
  }

  @Test
  public void fail_when_not_enough_permission() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    userSession.logIn(db.users().insertUser());

    final TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }
}
