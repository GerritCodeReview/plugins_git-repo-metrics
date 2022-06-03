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

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.testing.InMemoryModule;
import com.google.gerrit.testing.InMemoryRepositoryManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GitUpdateListenerTest {
  private final String pluginName = "git-repo-metrics";
  private final GitRepositoryManager repoManager = new InMemoryRepositoryManager();
  private final ExecutorService mockedExecutorService = mock(ExecutorService.class);
  private GitRepoUpdateListener gitRepoUpdateListener;
  private final String testProject = "testProject";
  private final Project.NameKey testProjectNameKey = Project.nameKey("testProject");
  private Repository repository;
  ArgumentCaptor<UpdateGitMetricsTask> valueCapture =
      ArgumentCaptor.forClass(UpdateGitMetricsTask.class);

  @Inject private UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;

  @Before
  public void setupRepo() throws IOException {

    AbstractModule m =
        new AbstractModule() {
          @Override
          protected void configure() {
            install(new InMemoryModule());
            install(new UpdateGitMetricsTaskModule());
            bind(new TypeLiteral<String>() {})
                .annotatedWith(PluginName.class)
                .toInstance(pluginName);
          }
        };
    Injector injector = Guice.createInjector(m);
    injector.injectMembers(this);

    reset(mockedExecutorService);
    gitRepoUpdateListener =
        new GitRepoUpdateListener(mockedExecutorService, updateGitMetricsTaskFactory);
    repository = repoManager.createRepository(testProjectNameKey);
  }

  @Test
  public void shouldUpdateMetrics() {
    doNothing().when(mockedExecutorService).execute(valueCapture.capture());
    gitRepoUpdateListener.onGitReferenceUpdated(new TestEvent(testProject));
    UpdateGitMetricsTask expectedUpdateGitMetricsTask =
        updateGitMetricsTaskFactory.create(testProject);
    assertThat(valueCapture.getValue().toString())
        .isEqualTo(expectedUpdateGitMetricsTask.toString());
  }

  public static class TestEvent implements GitReferenceUpdatedListener.Event {
    private final String projectName;

    protected TestEvent(String projectName) {
      this.projectName = projectName;
    }

    @Override
    public String getProjectName() {
      return projectName;
    }

    @Override
    public NotifyHandling getNotify() {
      return null;
    }

    @Override
    public String getRefName() {
      return null;
    }

    @Override
    public String getOldObjectId() {
      return null;
    }

    @Override
    public String getNewObjectId() {
      return null;
    }

    @Override
    public boolean isCreate() {
      return false;
    }

    @Override
    public boolean isDelete() {
      return false;
    }

    @Override
    public boolean isNonFastForward() {
      return false;
    }

    @Override
    public AccountInfo getUpdater() {
      return null;
    }
  }
}
