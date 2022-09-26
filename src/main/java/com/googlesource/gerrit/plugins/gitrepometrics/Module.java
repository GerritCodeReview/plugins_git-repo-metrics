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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.Scopes;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.FSMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStatsMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.util.concurrent.ExecutorService;

public class Module extends LifecycleModule {

  @Override
  protected void configure() {
    bind(GitRepoMetricsCache.class).in(Scopes.SINGLETON);
    bind(ExecutorService.class)
        .annotatedWith(UpdateGitMetricsExecutor.class)
        .toProvider(UpdateGitMetricsExecutorProvider.class);
    bind(GitRepoUpdateListener.class);
    DynamicSet.bind(binder(), EventListener.class).to(GitRepoUpdateListener.class);

    DynamicSet.setOf(binder(), MetricsCollector.class);
    DynamicSet.bind(binder(), MetricsCollector.class).to(GitStatsMetricsCollector.class);
    DynamicSet.bind(binder(), MetricsCollector.class).to(FSMetricsCollector.class);
    install(new UpdateGitMetricsTaskModule());
  }
}
