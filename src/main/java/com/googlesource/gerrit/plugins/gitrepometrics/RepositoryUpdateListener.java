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

import com.google.inject.Inject;
import java.util.concurrent.ExecutorService;

public abstract class RepositoryUpdateListener {

  protected final ExecutorService executor;
  protected final UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;
  protected final GitRepoMetricsCache gitRepoMetricsCache;

  @Inject
  RepositoryUpdateListener(
      @UpdateGitMetricsExecutor ExecutorService executor,
      UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory,
      GitRepoMetricsCache gitRepoMetricsCache) {
    this.executor = executor;
    this.updateGitMetricsTaskFactory = updateGitMetricsTaskFactory;
    this.gitRepoMetricsCache = gitRepoMetricsCache;
  }

  protected void maybeExecuteTask(String projectName) {
    if (gitRepoMetricsCache.shouldCollectStats(projectName)) {
      UpdateGitMetricsTask updateGitMetricsTask = updateGitMetricsTaskFactory.create(projectName);
      executor.execute(updateGitMetricsTask);
    }
  }
}
