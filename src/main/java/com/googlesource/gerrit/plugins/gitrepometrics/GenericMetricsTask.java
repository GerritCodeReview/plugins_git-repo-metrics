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

import java.util.HashMap;
import java.util.Map;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;

public class GenericMetricsTask implements Runnable, LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private GitRepoMetricsCache gitRepoMetricsCache;

  @Inject
  GenericMetricsTask(GitRepoMetricsCache gitRepoMetricsCache) {
    this.gitRepoMetricsCache = gitRepoMetricsCache;
  }

  @Override
  public void start() {
    logger.atInfo().log("Collecting initial generic metrics");
    run();
  }

  @Override
  public void stop() {}

  @Override
  public void run() {
    gitRepoMetricsCache.getGenericCollectors().stream()
        .forEach(
            metricsCollector -> {
              metricsCollector.collect(
                  metrics -> {
                    Map<GitRepoMetric, Long> newMetrics = new HashMap<>();
                    metrics.forEach(
                        (repoMetric, value) -> {
                          logger.atFine().log("Collected %s: %d", repoMetric.getName(), value);
                          newMetrics.put(repoMetric, value);
                        });
                    gitRepoMetricsCache.setMetrics(newMetrics);
                  });
            });
  }
}
