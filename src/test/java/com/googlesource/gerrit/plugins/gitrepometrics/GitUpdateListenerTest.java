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

import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class GitUpdateListenerTest extends RepositoryUpdateListenerTest {

  private GitRepoUpdateListener gitRepoUpdateListener;

  @Before
  public void setupRepo() throws IOException {
    testSetup();
    gitRepoUpdateListener =
        new GitRepoUpdateListener(
            mockedExecutorService, updateGitMetricsTaskFactory, gitRepoMetricsCache);
  }

  @Test
  public void shouldUpdateMetricsIfProjectIsEnabled() {
    gitRepoUpdateListener.onGitReferenceUpdated(new TestEvent(enabledProject));
    assertMetricsAreUpdated();
  }

  @Test
  public void shouldNotUpdateMetricsIfProjectIsDisabled() {
    gitRepoUpdateListener.onGitReferenceUpdated(new TestEvent(disabledProject));
    assertMetricsAreNotUpdated();
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
