// Copyright (C) 2025 The Android Open Source Project
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
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;

import java.util.concurrent.ScheduledExecutorService;

public class MetricsInitializer implements LifecycleListener {

  private final ScheduledExecutorService metricsExecutor;
  private ProjectCache projectCache;
  private final UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;


  @Inject
  public MetricsInitializer(@UpdateGitMetricsExecutor ScheduledExecutorService metricsExecutor, ProjectCache projectCache, UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory) {
    this.metricsExecutor = metricsExecutor;
    this.projectCache = projectCache;
    this.updateGitMetricsTaskFactory = updateGitMetricsTaskFactory;
  }

  @Override
  public void start() {
    projectCache.all().stream().map(Project.NameKey::get)
        .map(updateGitMetricsTaskFactory::create)
        .forEach(metricsExecutor::execute);
  }

  @Override
  public void stop() {}
}
