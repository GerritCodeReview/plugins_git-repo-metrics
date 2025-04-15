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
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.inject.Inject;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

class GitRepoUpdateListener implements EventListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  protected static final String REF_REPLICATED_EVENT_SUFFIX = "ref-replicated";
  private final ExecutorService executor;
  private final UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;
  private final GitRepoMetricsCache gitRepoMetricsCache;
  private final String instanceId;

  @Inject
  protected GitRepoUpdateListener(
      @GerritInstanceId String instanceId,
      @UpdateGitMetricsExecutor ScheduledExecutorService executor,
      UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory,
      GitRepoMetricsCache gitRepoMetricsCache) {
    this.instanceId = instanceId;
    this.executor = executor;
    this.updateGitMetricsTaskFactory = updateGitMetricsTaskFactory;
    this.gitRepoMetricsCache = gitRepoMetricsCache;
  }

  @Override
  public void onEvent(Event event) {
    if (isMyEvent(event) && (isRefUpdatedEvent(event) || isRefReplicatedEvent(event))) {
      String projectName = ((ProjectEvent) event).getProjectNameKey().get();
      logger.atFine().log(
          "Got %s event from %s. Might need to collect metrics for project %s",
          event.type, event.instanceId, projectName);

      gitRepoMetricsCache.collectStatsForProject(projectName);
    }
  }

  private boolean isRefReplicatedEvent(Event event) {
    // Check the name of the event instead of checking the class type
    // to avoid importing pull and push replication plugin dependencies
    // only for this check.

    return event.type.endsWith(REF_REPLICATED_EVENT_SUFFIX);
  }

  private boolean isRefUpdatedEvent(Event event) {
    return event.type.equals(RefUpdatedEvent.TYPE);
  }

  private boolean isMyEvent(Event event) {
    return event.type != null && Objects.equals(event.instanceId, instanceId);
  }
}
