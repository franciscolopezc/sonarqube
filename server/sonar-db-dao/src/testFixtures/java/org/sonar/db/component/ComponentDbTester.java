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
package org.sonar.db.component;

import java.util.Arrays;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.portfolio.PortfolioProjectDto;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.NONE;

public class ComponentDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public ComponentDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public SnapshotDto insertProjectAndSnapshot(ComponentDto component) {
    insertComponentAndBranchAndProject(component, null, defaults(), defaults(), defaults());
    return insertSnapshot(component);
  }

  public SnapshotDto insertPortfolioAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component, true);
    return insertSnapshot(component);
  }

  public ComponentDto insertComponent(ComponentDto component) {
    return insertComponentImpl(component, null, defaults());
  }

  public ComponentDto insertPrivateProject() {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true,
      defaults(), defaults(), defaults());
  }

  public BranchDto getBranchDto(ComponentDto branch) {
    return db.getDbClient().branchDao().selectByUuid(dbSession, branch.uuid())
      .orElseThrow(() -> new IllegalStateException("Project has invalid configuration"));
  }

  public ProjectDto getProjectDto(ComponentDto project) {
    return db.getDbClient().projectDao().selectByUuid(dbSession, project.uuid())
      .orElseThrow(() -> new IllegalStateException("Project has invalid configuration"));
  }

  public ComponentDto getComponentDto(ProjectDto project) {
    return db.getDbClient().componentDao().selectByUuid(dbSession, project.getUuid())
      .orElseThrow(() -> new IllegalStateException("Can't find project"));
  }

  public ComponentDto getComponentDto(BranchDto branch) {
    return db.getDbClient().componentDao().selectByUuid(dbSession, branch.getUuid())
      .orElseThrow(() -> new IllegalStateException("Can't find branch"));
  }

  public ComponentDto insertPrivateProject(ComponentDto componentDto) {
    return insertComponentAndBranchAndProject(componentDto, true);
  }

  public ComponentDto insertPublicProject() {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false);
  }

  public ComponentDto insertPublicProject(String uuid) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(uuid), false);
  }

  public ComponentDto insertPublicProject(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(uuid), false, defaults(), dtoPopulator);
  }

  public ComponentDto insertPublicProject(ComponentDto componentDto) {
    return insertComponentAndBranchAndProject(componentDto, false);
  }

  public ComponentDto insertPrivateProject(String uuid) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(uuid), true);
  }

  public final ComponentDto insertPrivateProject(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, defaults(), dtoPopulator);
  }

  public final ComponentDto insertPrivateProject(Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(),
      true, defaults(), componentDtoPopulator, projectDtoPopulator);
  }

  public final ComponentDto insertPublicProject(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, defaults(), dtoPopulator);
  }

  public final ComponentDto insertPublicProject(Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, defaults(), componentDtoPopulator, projectDtoPopulator);
  }

  public ProjectDto insertPublicProjectDto() {
    ComponentDto componentDto = insertPublicProject();
    return getProjectDto(componentDto);
  }

  public ProjectDto insertPrivateProjectDto() {
    ComponentDto componentDto = insertPrivateProject();
    return getProjectDto(componentDto);
  }

  public ProjectDto insertPublicProjectDto(Consumer<ComponentDto> dtoPopulator) {
    ComponentDto componentDto = insertPublicProject(dtoPopulator);
    return getProjectDto(componentDto);
  }

  public final ProjectDto insertPrivateProjectDto(String uuid) {
    ComponentDto componentDto = insertPrivateProject(uuid);
    return getProjectDto(componentDto);
  }

  public final ProjectDto insertPrivateProjectDto(Consumer<ComponentDto> componentDtoPopulator) {
    return insertPrivateProjectDto(componentDtoPopulator, defaults());
  }

  public final ProjectDto insertPrivateProjectDto(Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    ComponentDto componentDto = insertPrivateProject(componentDtoPopulator, projectDtoPopulator);
    return getProjectDto(componentDto);
  }

  public ProjectDto insertPrivateProjectDto(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    ComponentDto componentDto = insertPrivateProjectWithCustomBranch(branchPopulator, componentDtoPopulator, projectDtoPopulator);
    return getProjectDto(componentDto);
  }

  public final ComponentDto insertFile(ProjectDto project) {
    ComponentDto projectComponent = getComponentDto(project);
    return insertComponent(ComponentTesting.newFileDto(projectComponent));
  }

  public final ComponentDto insertFile(BranchDto branch) {
    ComponentDto projectComponent = getComponentDto(branch);
    return insertComponent(ComponentTesting.newFileDto(projectComponent));
  }

  public final ComponentDto insertPrivateProject(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(uuid), true, defaults(), dtoPopulator);
  }

  public final ComponentDto insertPrivateProjectWithCustomBranch(String branchKey) {
    return insertPrivateProjectWithCustomBranch(b -> b.setBranchType(BRANCH).setKey(branchKey), defaults());
  }

  public final ComponentDto insertPrivateProjectWithCustomBranch(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, branchPopulator, componentPopulator);
  }

  public final ComponentDto insertPublicProjectWithCustomBranch(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, branchPopulator, componentPopulator);
  }

  public final ComponentDto insertPrivateProjectWithCustomBranch(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentPopulator,
    Consumer<ProjectDto> projectPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, branchPopulator, componentPopulator, projectPopulator);
  }

  public final ComponentDto insertPublicPortfolio() {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, defaults(), defaults());
  }

  public final ComponentDto insertPublicPortfolio(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(false), false, dtoPopulator, defaults());
  }

  public final ComponentDto insertPublicPortfolio(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, defaults());
  }

  public final ComponentDto insertPublicPortfolio(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, portfolioPopulator);
  }

  public final PortfolioDto insertPublicPortfolioDto() {
    return insertPublicPortfolioDto(defaults());
  }

  public final PortfolioDto insertPublicPortfolioDto(Consumer<ComponentDto> dtoPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(String uuid) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(true), true, defaults(), defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(String uuid, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(true), true, defaults(), portfolioPopulator);
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto() {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, defaults(), defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(Consumer<ComponentDto> dtoPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, portfolioPopulator);
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPublicPortfolioDto(String uuid, Consumer<ComponentDto> dtoPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(false), false, dtoPopulator, defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPublicPortfolioDto(String uuid, Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(false), false, dtoPopulator, portfolioPopulator);
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPublicPortfolioDto(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, portfolioPopulator);
    return getPortfolioDto(component);
  }

  public PortfolioDto getPortfolioDto(ComponentDto portfolio) {
    return db.getDbClient().portfolioDao().selectByUuid(dbSession, portfolio.uuid())
      .orElseThrow(() -> new IllegalStateException("Portfolio has invalid configuration"));
  }

  public ComponentDto insertComponentAndPortfolio(ComponentDto componentDto, boolean isPrivate, Consumer<ComponentDto> componentPopulator,
    Consumer<PortfolioDto> portfolioPopulator) {
    insertComponentImpl(componentDto, isPrivate, componentPopulator);

    PortfolioDto portfolioDto = toPortfolioDto(componentDto, System2.INSTANCE.now());
    portfolioPopulator.accept(portfolioDto);
    dbClient.portfolioDao().insert(dbSession, portfolioDto);
    db.commit();
    return componentDto;
  }

  public final ComponentDto insertPrivatePortfolio() {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, defaults(), defaults());
  }

  public final ComponentDto insertPrivatePortfolio(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(true), true, dtoPopulator, defaults());
  }

  public final ComponentDto insertPrivatePortfolio(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, defaults());
  }

  public final ComponentDto insertPrivatePortfolio(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, portfolioPopulator);
  }

  public final ComponentDto insertSubportfolio(ComponentDto parentPortfolio) {
    ComponentDto subPortfolioComponent = ComponentTesting.newSubPortfolio(parentPortfolio);
    return insertComponentAndPortfolio(subPortfolioComponent, true, defaults(), sp -> sp.setParentUuid(sp.getRootUuid()));
  }

  public void addPortfolioReference(String portfolioUuid, String... referencerUuids) {
    for (String uuid : referencerUuids) {
      dbClient.portfolioDao().addReference(dbSession, portfolioUuid, uuid);
    }
    db.commit();
  }

  public void addPortfolioReference(ComponentDto portfolio, String... referencerUuids) {
    addPortfolioReference(portfolio.uuid(), referencerUuids);
  }

  public void addPortfolioReference(PortfolioDto portfolio, String... referencerUuids) {
    addPortfolioReference(portfolio.getUuid(), referencerUuids);
  }

  public void addPortfolioReference(PortfolioDto portfolio, PortfolioDto reference) {
    addPortfolioReference(portfolio.getUuid(), reference.getUuid());
  }

  public void addPortfolioProject(String portfolioUuid, String... projectUuids) {
    for (String uuid : projectUuids) {
      dbClient.portfolioDao().addProject(dbSession, portfolioUuid, uuid);
    }
    db.commit();
  }

  public void addPortfolioProject(ComponentDto portfolio, String... projectUuids) {
    addPortfolioProject(portfolio.uuid(), projectUuids);
  }

  public void addPortfolioProject(ComponentDto portfolio, ComponentDto... projects) {
    addPortfolioProject(portfolio, Arrays.stream(projects).map(ComponentDto::uuid).toArray(String[]::new));
  }

  public void addPortfolioProject(PortfolioDto portfolioDto, ProjectDto... projects) {
    for (ProjectDto project : projects) {
      dbClient.portfolioDao().addProject(dbSession, portfolioDto.getUuid(), project.getUuid());
    }
    db.commit();
  }

  public void addPortfolioProjectBranch(PortfolioDto portfolio, ProjectDto project, String branchUuid) {
    addPortfolioProjectBranch(portfolio, project.getUuid(), branchUuid);
  }

  public void addPortfolioProjectBranch(PortfolioDto portfolio, String projectUuid, String branchUuid) {
    addPortfolioProjectBranch(portfolio.getUuid(), projectUuid, branchUuid);
  }

  public void addPortfolioProjectBranch(String portfolioUuid, String projectUuid, String branchUuid) {
    PortfolioProjectDto portfolioProject = dbClient.portfolioDao().selectPortfolioProjectOrFail(dbSession, portfolioUuid, projectUuid);
    dbClient.portfolioDao().addBranch(db.getSession(), portfolioProject.getUuid(), branchUuid);
    db.commit();
  }

  public final ComponentDto insertPublicApplication() {
    return insertPublicApplication(defaults());
  }

  public final ComponentDto insertPublicApplication(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newApplication().setPrivate(false), false, defaults(), dtoPopulator);
  }

  public final ProjectDto insertPrivateApplicationDto() {
    return getProjectDto(insertPrivateApplication());
  }

  public final ProjectDto insertPublicApplicationDto() {
    return getProjectDto(insertPublicApplication());
  }

  public final ProjectDto insertPublicApplicationDto(Consumer<ComponentDto> dtoPopulator) {
    return getProjectDto(insertPublicApplication(dtoPopulator));
  }

  public final ProjectDto insertPrivateApplicationDto(Consumer<ComponentDto> dtoPopulator) {
    return getProjectDto(insertPrivateApplication(dtoPopulator, defaults()));
  }

  public final ProjectDto insertPrivateApplicationDto(Consumer<ComponentDto> dtoPopulator, Consumer<ProjectDto> appPopulator) {
    return getProjectDto(insertPrivateApplication(dtoPopulator, appPopulator));
  }

  public final ComponentDto insertPrivateApplication(Consumer<ComponentDto> dtoPopulator) {
    return insertPrivateApplication(dtoPopulator, defaults());
  }

  public final ComponentDto insertPrivateApplication() {
    return insertPrivateApplication(defaults(), defaults());
  }

  public final ComponentDto insertPrivateApplication(Consumer<ComponentDto> dtoPopulator, Consumer<ProjectDto> projectPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newApplication().setPrivate(true), true, defaults(), dtoPopulator, projectPopulator);
  }

  public final ComponentDto insertSubView(ComponentDto view) {
    return insertSubView(view, defaults());
  }

  public final ComponentDto insertSubView(ComponentDto view, Consumer<ComponentDto> dtoPopulator) {
    ComponentDto subViewComponent = ComponentTesting.newSubPortfolio(view);
    return insertComponentAndPortfolio(subViewComponent, view.isPrivate(), dtoPopulator, p -> p.setParentUuid(view.uuid()));
  }

  public void addPortfolioApplicationBranch(String portfolioUuid, String applicationUuid, String branchUuid) {
    dbClient.portfolioDao().addReferenceBranch(db.getSession(), portfolioUuid, applicationUuid, branchUuid);
    db.commit();
  }

  private ComponentDto insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate, Consumer<BranchDto> branchPopulator,
    Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    insertComponentImpl(component, isPrivate, componentDtoPopulator);

    ProjectDto projectDto = toProjectDto(component, System2.INSTANCE.now());
    projectDtoPopulator.accept(projectDto);
    dbClient.projectDao().insert(dbSession, projectDto);

    BranchDto branchDto = ComponentTesting.newMainBranchDto(component);
    branchDto.setExcludeFromPurge(true);
    branchPopulator.accept(branchDto);
    branchDto.setIsMain(true);
    dbClient.branchDao().insert(dbSession, branchDto);

    db.commit();
    return component;
  }

  public void addApplicationProject(ComponentDto application, ComponentDto... projects) {
    for (ComponentDto project : projects) {
      dbClient.applicationProjectsDao().addProject(dbSession, application.uuid(), project.uuid());
    }
    db.commit();
  }

  public void addApplicationProject(ProjectDto application, ProjectDto... projects) {
    for (ProjectDto project : projects) {
      dbClient.applicationProjectsDao().addProject(dbSession, application.getUuid(), project.getUuid());
    }
    db.commit();
  }

  public void addProjectBranchToApplicationBranch(ComponentDto applicationBranchComponent, ComponentDto... projectBranchesComponent) {
    BranchDto applicationBranch = getBranchDto(applicationBranchComponent);
    BranchDto[] componentDtos = Arrays.stream(projectBranchesComponent).map(this::getBranchDto).toArray(BranchDto[]::new);

    addProjectBranchToApplicationBranch(applicationBranch, componentDtos);
  }

  public void addProjectBranchToApplicationBranch(BranchDto applicationBranch, BranchDto... projectBranches) {
    for (BranchDto projectBranch : projectBranches) {
      dbClient.applicationProjectsDao().addProjectBranchToAppBranch(dbSession, applicationBranch, projectBranch);
    }
    db.commit();
  }

  private ComponentDto insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate, Consumer<BranchDto> branchPopulator,
    Consumer<ComponentDto> componentDtoPopulator) {
    return insertComponentAndBranchAndProject(component, isPrivate, branchPopulator, componentDtoPopulator, defaults());
  }

  private ComponentDto insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate, Consumer<BranchDto> branchPopulator) {
    return insertComponentAndBranchAndProject(component, isPrivate, branchPopulator, defaults());
  }

  private ComponentDto insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate) {
    return insertComponentAndBranchAndProject(component, isPrivate, defaults());
  }

  private ComponentDto insertComponentImpl(ComponentDto component, @Nullable Boolean isPrivate, Consumer<ComponentDto> dtoPopulator) {
    dtoPopulator.accept(component);
    checkState(isPrivate == null || component.isPrivate() == isPrivate, "Illegal modification of private flag");
    dbClient.componentDao().insert(dbSession, component, true);
    db.commit();

    return component;
  }

  public void insertComponents(ComponentDto... components) {
    dbClient.componentDao().insert(dbSession, asList(components), true);
    db.commit();
  }

  public SnapshotDto insertSnapshot(SnapshotDto snapshotDto) {
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, snapshotDto);
    db.commit();
    return snapshot;
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto) {
    return insertSnapshot(componentDto, defaults());
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(componentDto);
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public SnapshotDto insertSnapshot(BranchDto branchDto) {
    return insertSnapshot(branchDto, defaults());
  }

  public SnapshotDto insertSnapshot(ProjectDto project) {
    return insertSnapshot(project, defaults());
  }

  public SnapshotDto insertSnapshot(ProjectDto project, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(project.getUuid());
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public SnapshotDto insertSnapshot(BranchDto branchDto, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(branchDto);
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public void insertSnapshots(SnapshotDto... snapshotDtos) {
    dbClient.snapshotDao().insert(dbSession, asList(snapshotDtos));
    db.commit();
  }

  @SafeVarargs
  public final ComponentDto insertProjectBranch(ComponentDto project, Consumer<BranchDto>... dtoPopulators) {
    BranchDto branchDto = ComponentTesting.newBranchDto(project.uuid(), BRANCH);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(branchDto));
    return insertProjectBranch(project, branchDto);
  }

  @SafeVarargs
  public final BranchDto insertProjectBranch(ProjectDto project, Consumer<BranchDto>... dtoPopulators) {
    BranchDto branchDto = ComponentTesting.newBranchDto(project.getUuid(), BRANCH);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(branchDto));
    insertProjectBranch(project, branchDto);
    return branchDto;
  }

  public final ComponentDto insertProjectBranch(ProjectDto project, BranchDto branchDto) {
    checkArgument(branchDto.getProjectUuid().equals(project.getUuid()));
    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return branch;
  }

  public final ComponentDto insertProjectBranch(ComponentDto project, BranchDto branchDto) {
    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return branch;
  }

  // TODO temporary constructor to quickly create project from previous project component.
  public static ProjectDto toProjectDto(ComponentDto componentDto, long createTime) {
    return new ProjectDto()
      .setUuid(componentDto.uuid())
      .setKey(componentDto.getKey())
      .setQualifier(componentDto.qualifier() != null ? componentDto.qualifier() : Qualifiers.PROJECT)
      .setCreatedAt(createTime)
      .setUpdatedAt(createTime)
      .setPrivate(componentDto.isPrivate())
      .setDescription(componentDto.description())
      .setName(componentDto.name());
  }

  public static PortfolioDto toPortfolioDto(ComponentDto componentDto, long createTime) {
    return new PortfolioDto()
      .setUuid(componentDto.uuid())
      .setKey(componentDto.getKey())
      .setRootUuid(componentDto.branchUuid())
      .setSelectionMode(NONE.name())
      .setCreatedAt(createTime)
      .setUpdatedAt(createTime)
      .setPrivate(componentDto.isPrivate())
      .setDescription(componentDto.description())
      .setName(componentDto.name());
  }

  public static <T> Consumer<T> defaults() {
    return t -> {
    };
  }
}
