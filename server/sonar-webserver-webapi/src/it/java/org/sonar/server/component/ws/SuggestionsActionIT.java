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
package org.sonar.server.component.ws;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Components.SuggestionsWsResponse;
import org.sonarqube.ws.Components.SuggestionsWsResponse.Category;
import org.sonarqube.ws.Components.SuggestionsWsResponse.Suggestion;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.SUBVIEW;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newPublicProjectDto;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_MORE;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_QUERY;
import static org.sonar.server.component.ws.SuggestionsAction.PARAM_RECENTLY_BROWSED;
import static org.sonar.server.component.ws.SuggestionsAction.SHORT_INPUT_WARNING;
import static org.sonar.test.JsonAssert.assertJson;

public class SuggestionsActionIT {
  private static final String[] SUGGESTION_QUALIFIERS = Stream.of(SuggestionCategory.values())
    .map(SuggestionCategory::getQualifier)
    .collect(MoreCollectors.toList()).toArray(new String[0]);

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public final EsTester es = EsTester.create();
  @Rule
  public final UserSessionRule userSessionRule = UserSessionRule.standalone();
  public final ResourceTypesRule resourceTypes = new ResourceTypesRule();

  private final ComponentIndexer componentIndexer = new ComponentIndexer(db.getDbClient(), es.client());
  private final FavoriteFinder favoriteFinder = mock(FavoriteFinder.class);
  private final ComponentIndex index = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(userSessionRule), System2.INSTANCE);
  private final SuggestionsAction underTest = new SuggestionsAction(db.getDbClient(), index, favoriteFinder, userSessionRule, resourceTypes);
  private final PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, componentIndexer);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    resourceTypes.setAllQualifiers(SUGGESTION_QUALIFIERS);
  }

  @Test
  public void define_suggestions_action() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder(
      PARAM_MORE,
      PARAM_QUERY,
      PARAM_RECENTLY_BROWSED);
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("10.0", "The use of 'BRC' as value for parameter 'more' is no longer supported"),
      tuple("8.4", "The use of 'DIR', 'FIL','UTS' as values for parameter 'more' is no longer supported"),
      tuple("7.6", "The use of 'BRC' as value for parameter 'more' is deprecated"));

    WebService.Param recentlyBrowsed = action.param(PARAM_RECENTLY_BROWSED);
    assertThat(recentlyBrowsed.since()).isEqualTo("6.4");
    assertThat(recentlyBrowsed.exampleValue()).isNotEmpty();
    assertThat(recentlyBrowsed.description()).isNotEmpty();
    assertThat(recentlyBrowsed.isRequired()).isFalse();

    WebService.Param query = action.param(PARAM_QUERY);
    assertThat(query.exampleValue()).isNotEmpty();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.isRequired()).isFalse();
  }

  @Test
  public void test_example_json_response() {
    ComponentDto project1 = db.components().insertPublicProject(p -> p.setKey("org.sonarsource:sonarqube").setName("SonarSource :: SonarQube"));
    ComponentDto project2 = db.components().insertPublicProject(p -> p.setKey("org.sonarsource:sonarlint").setName("SonarSource :: SonarLint"));
    componentIndexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project1);
    authorizationIndexerTester.allowOnlyAnyone(project2);

    TestResponse wsResponse = ws.newRequest()
      .setParam(PARAM_QUERY, "Sonar")
      .setParam(PARAM_RECENTLY_BROWSED, project1.getKey())
      .setMethod("POST")
      .setMediaType(MediaTypes.JSON)
      .execute();

    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(wsResponse.getInput());
  }

  @Test
  public void suggestions_without_query_should_contain_recently_browsed() {
    ComponentDto project = db.components().insertPrivateProject();

    componentIndexer.indexAll();
    userSessionRule.addProjectPermission(USER, project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RECENTLY_BROWSED, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .containsExactly(PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getIsRecentlyBrowsed)
      .containsExactly(tuple(project.getKey(), true));
  }

  @Test
  public void suggestions_without_query_should_contain_recently_browsed_public_project() {
    ComponentDto project = db.components().insertPublicProject();

    componentIndexer.indexAll();

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RECENTLY_BROWSED, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .containsExactly(PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getIsRecentlyBrowsed)
      .containsExactly(tuple(project.getKey(), true));
  }

  @Test
  public void suggestions_without_query_should_not_contain_recently_browsed_without_permission() {
    ComponentDto project = db.components().insertPrivateProject();

    componentIndexer.indexAll();

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RECENTLY_BROWSED, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .isEmpty();
  }

  @Test
  public void suggestions_without_query_should_contain_favorites() {
    ComponentDto project = db.components().insertPrivateProject();
    doReturn(singletonList(project)).when(favoriteFinder).list();

    componentIndexer.indexAll();
    userSessionRule.addProjectPermission(USER, project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .containsExactly(PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getIsFavorite)
      .containsExactly(tuple(project.getKey(), true));
  }

  @Test
  public void suggestions_without_query_should_not_contain_favorites_without_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    doReturn(singletonList(project)).when(favoriteFinder).list();

    componentIndexer.indexAll();

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .isEmpty();
  }

  @Test
  public void suggestions_without_query_should_contain_recently_browsed_favorites() {
    ComponentDto project = db.components().insertPrivateProject();
    doReturn(singletonList(project)).when(favoriteFinder).list();

    componentIndexer.indexAll();
    userSessionRule.addProjectPermission(USER, project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RECENTLY_BROWSED, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .containsExactly(Qualifiers.PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getIsFavorite, Suggestion::getIsRecentlyBrowsed)
      .containsExactly(tuple(project.getKey(), true, true));
  }

  @Test
  public void suggestions_without_query_should_not_contain_matches_that_are_neither_favorites_nor_recently_browsed() {
    ComponentDto project = db.components().insertPrivateProject();

    componentIndexer.indexAll();
    userSessionRule.addProjectPermission(USER, project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .isEmpty();
  }

  @Test
  public void suggestions_without_query_should_order_results() {
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setName("Alpha"));
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setName("Bravo"));
    ComponentDto project3 = db.components().insertPrivateProject(p -> p.setName("Charlie"));
    ComponentDto project4 = db.components().insertPrivateProject(p -> p.setName("Delta"));
    doReturn(asList(project4, project2)).when(favoriteFinder).list();

    componentIndexer.indexAll();
    userSessionRule.addProjectPermission(USER, project1);
    userSessionRule.addProjectPermission(USER, project2);
    userSessionRule.addProjectPermission(USER, project3);
    userSessionRule.addProjectPermission(USER, project4);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RECENTLY_BROWSED, Stream.of(project3, project1).map(ComponentDto::getKey).collect(joining(",")))
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert order of keys
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getName, Suggestion::getIsFavorite, Suggestion::getIsRecentlyBrowsed)
      .containsExactly(
        tuple("Bravo", true, false),
        tuple("Delta", true, false),
        tuple("Alpha", false, true),
        tuple("Charlie", false, true));
  }

  @Test
  public void suggestions_without_query_should_return_empty_qualifiers() {
    ComponentDto project = db.components().insertPrivateProject();
    componentIndexer.indexOnAnalysis(project.branchUuid());
    userSessionRule.addProjectPermission(USER, project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RECENTLY_BROWSED, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .extracting(Category::getQ, Category::getItemsCount)
      .containsExactlyInAnyOrder(tuple("VW", 0), tuple("APP", 0), tuple("SVW", 0), tuple("TRK", 1))
      .doesNotContain(tuple("BRC", 0), tuple("FIL", 0), tuple("UTS", 0));
  }

  @Test
  public void suggestions_should_filter_allowed_qualifiers() {
    resourceTypes.setAllQualifiers(PROJECT, FILE, UNIT_TEST_FILE);
    ComponentDto project = db.components().insertPrivateProject();
    componentIndexer.indexOnAnalysis(project.branchUuid());
    userSessionRule.addProjectPermission(USER, project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_RECENTLY_BROWSED, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .extracting(Category::getQ)
      .containsExactlyInAnyOrder(PROJECT).doesNotContain(FILE, UNIT_TEST_FILE);
  }

  @Test
  public void exact_match_in_one_qualifier() {
    ComponentDto project = db.components().insertPrivateProject();

    componentIndexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .extracting(Category::getQ)
      .containsExactly(PROJECT);

    // assert correct id to be found
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey)
      .containsExactly(project.getKey());
  }

  @Test
  public void should_not_return_suggestion_on_non_existing_project() {
    ComponentDto project = db.components().insertPrivateProject();

    componentIndexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project);

    db.getDbClient().purgeDao().deleteProject(db.getSession(), project.uuid(), PROJECT, project.name(), project.getKey());
    db.commit();

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, project.getKey())
      .executeProtobuf(SuggestionsWsResponse.class);

    // assert match in qualifier "TRK"
    assertThat(response.getResultsList())
      .filteredOn(q -> q.getItemsCount() > 0)
      .isEmpty();
  }

  @Test
  public void must_not_search_if_no_valid_tokens_are_provided() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName("SonarQube"));

    componentIndexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "S o")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList()).filteredOn(q -> q.getItemsCount() > 0).isEmpty();
    assertThat(response.getWarning()).contains(SHORT_INPUT_WARNING);
  }

  @Test
  public void should_warn_about_short_inputs() {
    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "validLongToken x")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getWarning()).contains(SHORT_INPUT_WARNING);
  }

  @Test
  public void should_warn_about_short_inputs_but_return_results_based_on_other_terms() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName("SonarQube"));

    componentIndexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Sonar Q")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey)
      .contains(project.getKey());
    assertThat(response.getWarning()).contains(SHORT_INPUT_WARNING);
  }

  @Test
  public void should_contain_component_names() {
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setName("Project1"));
    componentIndexer.indexOnAnalysis(project1.branchUuid());
    authorizationIndexerTester.allowOnlyAnyone(project1);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Project")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getName)
      .containsExactlyInAnyOrder(tuple(project1.getKey(), project1.name()));
  }

  @Test
  public void should_mark_recently_browsed_items() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName("ProjectTest"));
    ComponentDto file1 = newFileDto(project).setName("File1");
    ComponentDto file2 = newFileDto(project).setName("File2");
    componentIndexer.indexOnAnalysis(project.branchUuid());
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Test")
      .setParam(PARAM_RECENTLY_BROWSED, Stream.of(file1.getKey(), project.getKey()).collect(joining(",")))
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getIsRecentlyBrowsed)
      .containsExactly(true);
  }

  @Test
  public void should_mark_favorite_items() {
    ComponentDto favouriteProject = db.components().insertPrivateProject(p -> p.setName("Project1"));
    ComponentDto nonFavouriteProject = db.components().insertPublicProject(p -> p.setName("Project2"));

    doReturn(singletonList(favouriteProject)).when(favoriteFinder).list();
    componentIndexer.indexOnAnalysis(favouriteProject.branchUuid());
    componentIndexer.indexOnAnalysis(nonFavouriteProject.branchUuid());
    authorizationIndexerTester.allowOnlyAnyone(favouriteProject, nonFavouriteProject);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, "Project")
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .extracting(Suggestion::getKey, Suggestion::getIsFavorite)
      .containsExactly(tuple(favouriteProject.getKey(), true), tuple(nonFavouriteProject.getKey(), false));
  }

  @Test
  public void should_return_empty_qualifiers() {
    ComponentDto project = db.components().insertPrivateProject();
    componentIndexer.indexOnAnalysis(project.branchUuid());
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, project.name())
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .extracting(Category::getQ, Category::getItemsCount)
      .containsExactlyInAnyOrder(tuple("VW", 0), tuple("SVW", 0), tuple("APP", 0), tuple("TRK", 1));
  }

  @Test
  public void should_only_provide_project_for_certain_qualifiers() {
    String query = randomAlphabetic(10);

    ComponentDto app = db.components().insertPublicApplication(v -> v.setName(query));
    ComponentDto view = db.components().insertPublicPortfolio(v -> v.setName(query));
    ComponentDto subView = db.components().insertComponent(ComponentTesting.newSubPortfolio(view).setName(query));
    ComponentDto project = db.components().insertPrivateProject(p -> p.setName(query));
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "path").setName(query));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project, dir).setName(query));
    ComponentDto test = db.components().insertComponent(ComponentTesting.newFileDto(project, dir).setName(query).setQualifier(UNIT_TEST_FILE));
    componentIndexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project);
    authorizationIndexerTester.allowOnlyAnyone(view);
    authorizationIndexerTester.allowOnlyAnyone(app);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, project.name())
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .extracting(Category::getQ, c -> c.getItemsList().stream().map(Suggestion::hasProject).findFirst().orElse(null))
      .containsExactlyInAnyOrder(
        tuple(SuggestionCategory.APP.getName(), false),
        tuple(SuggestionCategory.VIEW.getName(), false),
        tuple(SuggestionCategory.SUBVIEW.getName(), false),
        tuple(SuggestionCategory.PROJECT.getName(), false));
  }

  @Test
  public void does_not_return_branches() {
    ComponentDto project = db.components().insertPublicProject();
    authorizationIndexerTester.allowOnlyAnyone(project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    componentIndexer.indexAll();
    authorizationIndexerTester.allowOnlyAnyone(project);

    SuggestionsWsResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUERY, project.name())
      .executeProtobuf(SuggestionsWsResponse.class);

    assertThat(response.getResultsList())
      .filteredOn(c -> "TRK".equals(c.getQ()))
      .extracting(Category::getItemsList)
      .hasSize(1);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_0_projects_are_found() {
    check_proposal_to_show_more_results(0, 0, 0L, null, true);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_0_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(0, 0, 0L, null, false);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_5_projects_are_found() {
    check_proposal_to_show_more_results(5, 5, 0L, null, true);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_5_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(5, 5, 0L, null, false);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_6_projects_are_found() {
    check_proposal_to_show_more_results(6, 6, 0L, null, true);
  }

  @Test
  public void should_not_propose_to_show_more_results_if_6_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(6, 6, 0L, null, false);
  }

  @Test
  public void should_propose_to_show_more_results_if_7_projects_are_found() {
    check_proposal_to_show_more_results(7, 6, 1L, null, true);
  }

  @Test
  public void should_propose_to_show_more_results_if_7_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(7, 6, 1L, null, false);
  }

  @Test
  public void show_more_results_if_requested_and_5_projects_are_found() {
    check_proposal_to_show_more_results(5, 0, 0L, SuggestionCategory.PROJECT, true);
  }

  @Test
  public void show_more_results_if_requested_and_5_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(5, 0, 0L, SuggestionCategory.PROJECT, false);
  }

  @Test
  public void show_more_results_if_requested_and_6_projects_are_found() {
    check_proposal_to_show_more_results(6, 0, 0L, SuggestionCategory.PROJECT, true);
  }

  @Test
  public void show_more_results_if_requested_and_6_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(6, 0, 0L, SuggestionCategory.PROJECT, false);
  }

  @Test
  public void show_more_results_if_requested_and_7_projects_are_found() {
    check_proposal_to_show_more_results(7, 1, 0L, SuggestionCategory.PROJECT, true);
  }

  @Test
  public void show_more_results_if_requested_and_7_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(7, 1, 0L, SuggestionCategory.PROJECT, false);
  }

  @Test
  public void show_more_results_if_requested_and_26_projects_are_found() {
    check_proposal_to_show_more_results(26, 20, 0L, SuggestionCategory.PROJECT, true);
  }

  @Test
  public void show_more_results_if_requested_and_26_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(26, 20, 0L, SuggestionCategory.PROJECT, false);
  }

  @Test
  public void show_more_results_if_requested_and_27_projects_are_found() {
    check_proposal_to_show_more_results(27, 20, 1L, SuggestionCategory.PROJECT, true);
  }

  @Test
  public void show_more_results_if_requested_and_27_projects_are_found_and_no_search_query_is_provided() {
    check_proposal_to_show_more_results(27, 20, 1L, SuggestionCategory.PROJECT, false);
  }

  @Test
  public void show_more_results_filter_out_if_non_allowed_qualifiers() {
    resourceTypes.setAllQualifiers(APP, VIEW, SUBVIEW);

    check_proposal_to_show_more_results(10, 0, 0L, SuggestionCategory.PROJECT, true);
  }

  private void check_proposal_to_show_more_results(int numberOfProjects, int expectedNumberOfResults, long expectedNumberOfMoreResults, @Nullable SuggestionCategory more,
    boolean useQuery) {
    String namePrefix = "MyProject";

    List<ComponentDto> projects = range(0, numberOfProjects)
      .mapToObj(i -> db.components().insertPublicProject(p -> p.setName(namePrefix + i)))
      .collect(Collectors.toList());

    componentIndexer.indexAll();
    projects.forEach(authorizationIndexerTester::allowOnlyAnyone);

    TestRequest request = ws.newRequest()
      .setMethod("POST");
    if (useQuery) {
      request.setParam(PARAM_QUERY, namePrefix);
    } else {
      doReturn(projects).when(favoriteFinder).list();
    }
    ofNullable(more).ifPresent(c -> request.setParam(PARAM_MORE, c.getName()));
    SuggestionsWsResponse response = request
      .executeProtobuf(SuggestionsWsResponse.class);

    // include limited number of results in the response
    assertThat(response.getResultsList())
      .flatExtracting(Category::getItemsList)
      .hasSize(expectedNumberOfResults);

    // indicate, that there are more results
    if (expectedNumberOfResults == 0 && expectedNumberOfMoreResults == 0) {
      assertThat(response.getResultsList())
        .filteredOn(q -> q.getItemsCount() > 0)
        .isEmpty();
    } else {
      assertThat(response.getResultsList())
        .filteredOn(c -> "TRK".equals(c.getQ()))
        .extracting(Category::getMore)
        .containsExactly(expectedNumberOfMoreResults);
      response.getResultsList().stream()
        .filter(c -> !"TRK".equals(c.getQ()))
        .map(Category::getMore)
        .forEach(m -> assertThat(m).isEqualTo(0L));
    }
  }
}
