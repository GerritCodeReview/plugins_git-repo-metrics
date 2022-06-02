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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;

public class PluginStartup implements LifecycleListener {
  public final MetricMaker metricMaker;
  public final GitRepoMetricsConfig config;
  public final GitRepoMetricsCache gitRepoMetricsCache;

  @Inject
  public PluginStartup(
      MetricMaker metricMaker,
      GitRepoMetricsConfig config,
      GitRepoMetricsCache gitRepoMetricsCache) {
    this.metricMaker = metricMaker;
    this.config = config;
    this.gitRepoMetricsCache = gitRepoMetricsCache;
  }

  @Override
  public void start() {
    GitRepoMetricsCache.initCache(gitRepoMetricsCache);
  }

  @Override
  public void stop() {}
}
