// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.gitrepometrics;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.gitrepometrics.GitRepoUpdateListener.REF_REPLICATED_EVENT_SUFFIX;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.gerrit.server.events.RefEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.testing.InMemoryModule;
import com.google.gerrit.testing.InMemoryRepositoryManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GitUpdateListenerTest {
  private final GitRepositoryManager repoManager = new InMemoryRepositoryManager();
  private final ScheduledExecutorService mockedExecutorService =
      mock(ScheduledExecutorService.class);
  private GitRepoUpdateListener gitRepoUpdateListener;
  private final String enabledProject = "enabledProject";
  private final Project.NameKey enabledProjectNameKey = Project.nameKey(enabledProject);

  ArgumentCaptor<Runnable> updateGitMetricsTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
  private GitRepoMetricsCache gitRepoMetricsCache;
  private final String disabledProject = "disabledProject";
  private final Project.NameKey disabledProjectNameKey = Project.nameKey(disabledProject);
  private final String producerInstanceId = "producerInstanceId";

  @Inject private UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;

  @Before
  public void setupRepo() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledProject));
    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            new DynamicSet<>(),
            new DisabledMetricMaker(),
            new MetricRegistry(),
            configSetupUtils.getGitRepoMetricsConfig());

    AbstractModule m =
        new AbstractModule() {
          @Override
          protected void configure() {
            install(new InMemoryModule());
            install(new UpdateGitMetricsTaskModule());
            bind(new TypeLiteral<String>() {})
                .annotatedWith(PluginName.class)
                .toInstance(ConfigSetupUtils.pluginName);
            bind(GitRepoMetricsCache.class).toInstance(gitRepoMetricsCache);
          }
        };
    Injector injector = Guice.createInjector(m);
    injector.injectMembers(this);

    reset(mockedExecutorService);
    doNothing().when(mockedExecutorService).execute(updateGitMetricsTaskCaptor.capture());
    repoManager.createRepository(enabledProjectNameKey);
    repoManager.createRepository(disabledProjectNameKey);

    gitRepoUpdateListener =
        new GitRepoUpdateListener(
            producerInstanceId,
            mockedExecutorService,
            updateGitMetricsTaskFactory,
            gitRepoMetricsCache,
            new ProjectMetricsUnlimited());
  }

  @Test
  public void shouldUpdateMetricsIfProjectIsEnabledOnRefUpdated() {
    gitRepoUpdateListener.onEvent(getRefUpdatedEvent(enabledProject));
    assertMetricsUpdateTaskIsExecuted();
  }

  @Test
  public void shouldNotUpdateMetricsIfProjectIsDisabledOnRefUpdated() {
    gitRepoUpdateListener.onEvent(getRefUpdatedEvent(disabledProject));
    assertMetricsUpdateTaskIsNotExecuted();
  }

  @Test
  public void shouldNotUpdateMetricsOnRefReplicatedFromOtherNode() {
    gitRepoUpdateListener.onEvent(
        getRefReplicationEvent(
            REF_REPLICATED_EVENT_SUFFIX, enabledProject, "another-node-instance-id"));
    assertMetricsUpdateTaskIsNotExecuted();
  }

  @Test
  public void shouldNotUpdateMetricsOnRefUpdatedFromOtherNode() {
    gitRepoUpdateListener.onEvent(getRefUpdatedEvent(enabledProject, "another-node-instance-id"));
    assertMetricsUpdateTaskIsNotExecuted();
  }

  @Test
  public void shouldUpdateMetricsIfProjectIsEnabledOnRefReplicated() {
    gitRepoUpdateListener.onEvent(
        getRefReplicationEvent(REF_REPLICATED_EVENT_SUFFIX, enabledProject, producerInstanceId));
    assertMetricsUpdateTaskIsExecuted();
  }

  @Test
  public void shouldNotUpdateMetricsIfProjectIsDisabledOnOnRefReplicated() {
    gitRepoUpdateListener.onEvent(
        getRefReplicationEvent(REF_REPLICATED_EVENT_SUFFIX, disabledProject, producerInstanceId));
    assertMetricsUpdateTaskIsNotExecuted();
  }

  @Test
  public void shouldNotUpdateMetricsOnUnknownEvent() {
    gitRepoUpdateListener.onEvent(
        getRefReplicationEvent("any-event", enabledProject, producerInstanceId));
    assertMetricsUpdateTaskIsNotExecuted();
  }

  @Test
  public void shouldUpdateMetricsOnRefReplicatedFromSameNode() {
    gitRepoUpdateListener.onEvent(
        getRefReplicationEvent(REF_REPLICATED_EVENT_SUFFIX, enabledProject, producerInstanceId));
    assertMetricsUpdateTaskIsExecuted();
  }

  private RefUpdatedEvent getRefUpdatedEvent(String projectName) {
    return getRefUpdatedEvent(projectName, producerInstanceId);
  }

  private RefUpdatedEvent getRefUpdatedEvent(String projectName, String instanceId) {
    RefUpdatedEvent refUpdatedEvent = new RefUpdatedEvent();
    refUpdatedEvent.instanceId = instanceId;
    refUpdatedEvent.refUpdate =
        () -> {
          RefUpdateAttribute attributes = new RefUpdateAttribute();
          attributes.project = projectName;
          attributes.refName = "refs/for/master";
          return attributes;
        };
    return refUpdatedEvent;
  }

  private ReplicationTestEvent getRefReplicationEvent(
      String type, String projectName, String instanceId) {
    ReplicationTestEvent event = new ReplicationTestEvent(type, projectName);
    event.instanceId = instanceId;
    return event;
  }

  private static class ReplicationTestEvent extends RefEvent {
    private final String projectName;

    private ReplicationTestEvent(String type, String projectName) {
      super(type);
      this.projectName = projectName;
    }

    @Override
    public Project.NameKey getProjectNameKey() {
      return Project.NameKey.parse(projectName);
    }

    @Override
    public String getRefName() {
      return "refs/for/test";
    }
  }

  private void assertMetricsUpdateTaskIsExecuted() {
    assertThat(updateGitMetricsTaskCaptor.getValue()).isNotNull();
  }

  private void assertMetricsUpdateTaskIsNotExecuted() {
    updateGitMetricsTaskFactory.create(enabledProject);
    verifyNoInteractions(mockedExecutorService);
  }
}
