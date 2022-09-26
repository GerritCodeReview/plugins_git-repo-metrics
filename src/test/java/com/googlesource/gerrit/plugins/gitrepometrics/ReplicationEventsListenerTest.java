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

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.RefEvent;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class ReplicationEventsListenerTest extends RepositoryUpdateListenerTest {

  private ReplicationEventsListener replicationEventsListener;

  @Before
  public void setupListener() throws IOException {
    replicationEventsListener =
        new ReplicationEventsListener(
            mockedExecutorService, updateGitMetricsTaskFactory, gitRepoMetricsCache);
  }

  @Test
  public void shouldUpdateMetricsIfProjectIsEnabled() {
    replicationEventsListener.onEvent(
        new ReplicationTestEvent("ref-replication-done", enabledProject));
    assertMetricsAreUpdated();
  }

  @Test
  public void shouldNotUpdateMetricsIfProjectIsDisabled() {
    replicationEventsListener.onEvent(
        new ReplicationTestEvent("ref-replication-done", disabledProject));
    assertMetricsAreNotUpdated();
  }

  @Test
  public void shouldNotUpdateMetricsIfNoTReplicationDone() {
    replicationEventsListener.onEvent(new ReplicationTestEvent("any-event", enabledProject));
    assertMetricsAreNotUpdated();
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
}
