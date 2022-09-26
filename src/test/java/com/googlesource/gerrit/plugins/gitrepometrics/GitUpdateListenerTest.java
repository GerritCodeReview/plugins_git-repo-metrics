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
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GitUpdateListenerTest {
  private final GitRepositoryManager repoManager = new InMemoryRepositoryManager();

  protected final ExecutorService mockedExecutorService = mock(ExecutorService.class);

  protected final String enabledProject = "enabledProject";
  private final Project.NameKey enabledProjectNameKey = Project.nameKey(enabledProject);

  protected final String disabledProject = "disabledProject";
  private final Project.NameKey disabledProjectNameKey = Project.nameKey(disabledProject);

  private GitRepoUpdateListener gitRepoUpdateListener;

  ArgumentCaptor<UpdateGitMetricsTask> valueCapture =
      ArgumentCaptor.forClass(UpdateGitMetricsTask.class);
  protected GitRepoMetricsCache gitRepoMetricsCache;

  @Inject protected UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;

  @Before
  public void testSetup() throws IOException {
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
    doNothing().when(mockedExecutorService).execute(valueCapture.capture());
    repoManager.createRepository(enabledProjectNameKey);
    repoManager.createRepository(disabledProjectNameKey);

    gitRepoUpdateListener =
        new GitRepoUpdateListener(
            mockedExecutorService, updateGitMetricsTaskFactory, gitRepoMetricsCache);
  }

  @Test
  public void shouldUpdateMetricsIfProjectIsEnabledRefUpdated() {
    gitRepoUpdateListener.onEvent(getRefUpdatedEvent(enabledProject));
    assertMetricsAreUpdated();
  }

  @Test
  public void shouldNotUpdateMetricsIfProjectIsDisabledRefUpdated() {
    gitRepoUpdateListener.onEvent(getRefUpdatedEvent(disabledProject));
    assertMetricsAreNotUpdated();
  }

  @Test
  public void shouldUpdateMetricsIfProjectIsEnabledRefReplicationDone() {
    gitRepoUpdateListener.onEvent(new ReplicationTestEvent("ref-replication-done", enabledProject));
    assertMetricsAreUpdated();
  }

  @Test
  public void shouldNotUpdateMetricsIfProjectIsDisabledReplicationDone() {
    gitRepoUpdateListener.onEvent(
        new ReplicationTestEvent("ref-replication-done", disabledProject));
    assertMetricsAreNotUpdated();
  }

  @Test
  public void shouldNotUpdateMetricsIfNoTReplicationDone() {
    gitRepoUpdateListener.onEvent(new ReplicationTestEvent("any-event", enabledProject));
    assertMetricsAreNotUpdated();
  }

  private RefUpdatedEvent getRefUpdatedEvent(String projectName) {
    RefUpdatedEvent refUpdatedEvent = new RefUpdatedEvent();
    refUpdatedEvent.refUpdate =
        () -> {
          RefUpdateAttribute attributes = new RefUpdateAttribute();
          attributes.project = projectName;
          attributes.refName = "refs/for/master";
          return attributes;
        };
    return refUpdatedEvent;
  }

  public static class ReplicationTestEvent extends RefEvent {
    private final String projectName;

    protected ReplicationTestEvent(String type, String projectName) {
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

  protected void assertMetricsAreUpdated() {
    UpdateGitMetricsTask expectedUpdateGitMetricsTask =
        updateGitMetricsTaskFactory.create(enabledProject);
    assertThat(valueCapture.getValue().toString())
        .isEqualTo(expectedUpdateGitMetricsTask.toString());
  }

  protected void assertMetricsAreNotUpdated() {
    updateGitMetricsTaskFactory.create(enabledProject);
    verifyNoInteractions(mockedExecutorService);
  }
}
