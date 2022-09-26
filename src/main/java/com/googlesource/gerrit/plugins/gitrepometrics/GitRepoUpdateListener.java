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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.inject.Inject;
import java.util.concurrent.ExecutorService;

class GitRepoUpdateListener implements EventListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final ExecutorService executor;
  private final UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;
  private final GitRepoMetricsCache gitRepoMetricsCache;

  @Inject
  protected GitRepoUpdateListener(
      @UpdateGitMetricsExecutor ExecutorService executor,
      UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory,
      GitRepoMetricsCache gitRepoMetricsCache) {
    this.executor = executor;
    this.updateGitMetricsTaskFactory = updateGitMetricsTaskFactory;
    this.gitRepoMetricsCache = gitRepoMetricsCache;
  }

  @Override
  public void onEvent(Event event) {
    if (event instanceof RefUpdatedEvent || isReplicationDoneEvent(event)) {
      String projectName = ((ProjectEvent) event).getProjectNameKey().get();
      logger.atFine().log(
          String.format(
              "Got %s event from %s. Might need to collect metrics for project %s",
              event.type, event.instanceId, projectName));

      if (gitRepoMetricsCache.shouldCollectStats(projectName)) {
        UpdateGitMetricsTask updateGitMetricsTask = updateGitMetricsTaskFactory.create(projectName);
        executor.execute(updateGitMetricsTask);
      }
    }
  }

  private boolean isReplicationDoneEvent(Event event) {
    // Check the name of the event instead of checking the class type
    // to avoid importing pull and push replication plugin dependencies
    // only for this check.
    return event.type != null && event.type.endsWith("-replication-done");
  }
}
