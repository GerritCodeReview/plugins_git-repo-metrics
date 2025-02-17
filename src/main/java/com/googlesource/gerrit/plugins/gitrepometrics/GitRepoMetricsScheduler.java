// Copyright (C) 2024 The Android Open Source Project
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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class GitRepoMetricsScheduler implements LifecycleListener, Runnable {

  private final ScheduledExecutorService metricsExecutor;
  private final UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;
  private final Long gracePeriodMs;
  private ScheduledFuture<?> updaterTask;
  private List<String> repositoryNames;

  @Inject
  public GitRepoMetricsScheduler(
      @UpdateGitMetricsExecutor ScheduledExecutorService metricsExecutor,
      GitRepoMetricsConfig config,
      UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory) {
    this.metricsExecutor = metricsExecutor;
    repositoryNames = config.getRepositoryNames();
    gracePeriodMs = config.getGracePeriodMs();
    this.updateGitMetricsTaskFactory = updateGitMetricsTaskFactory;
  }

  @Override
  public void start() {
    updaterTask =
        metricsExecutor.scheduleAtFixedRate(
            this, 0, gracePeriodMs, TimeUnit.MILLISECONDS);
  }

  @Override
  public void stop() {
    updaterTask.cancel(true);
  }

  @Override
  public void run() {
    repositoryNames.stream()
        .map(updateGitMetricsTaskFactory::create)
        .forEach(metricsExecutor::execute);
  }
}
