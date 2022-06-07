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
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.inject.Inject;
import java.util.concurrent.ExecutorService;

public class GitRepoUpdateListener implements GitReferenceUpdatedListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final ExecutorService executor;
  private final UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;
  private final GitRepoMetricsCache gitRepoMetricsCache;

  @Inject
  GitRepoUpdateListener(
      @UpdateGitMetricsExecutor ExecutorService executor,
      UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory,
      GitRepoMetricsCache gitRepoMetricsCache) {
    this.executor = executor;
    this.updateGitMetricsTaskFactory = updateGitMetricsTaskFactory;
    this.gitRepoMetricsCache = gitRepoMetricsCache;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    String projectName = event.getProjectName();
    logger.atFine().log("Got an update for project %s", projectName);

    if (gitRepoMetricsCache.shouldCollectStats(projectName)) {
      UpdateGitMetricsTask updateGitMetricsTask = updateGitMetricsTaskFactory.create(projectName);
      executor.execute(updateGitMetricsTask);
    }
  }
}
