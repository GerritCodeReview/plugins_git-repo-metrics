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

import com.google.common.base.Supplier;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Singleton
public class GitRepoMetricsCache {
  private Map<String, Long> metrics;
  private final MetricMaker metricMaker;
  private final List<String> projects;

  public static List<GitRepoMetric> metricsNames = new ArrayList<>(GitStats.availableMetrics());

  @Inject
  GitRepoMetricsCache(MetricMaker metricMaker, GitRepoMetricsConfig config) {
    this.metricMaker = metricMaker;
    this.projects = config.getRepositoryNames();
    this.metrics = new HashMap<>(Collections.emptyMap());
  }

  public Map<String, Long> getMetrics() {
    return metrics;
  }

  public void setMetrics(Map<String, Long> metrics) {
    this.metrics = metrics;
  }

  public static void initCache(GitRepoMetricsCache gitRepoMetricsCache) {
    metricsNames.forEach(
        gitRepoMetric -> {
          gitRepoMetricsCache.projects.forEach(
              projectName -> {
                String name =
                    GitRepoMetricsCache.getMetricName(gitRepoMetric.getName(), projectName);
                Supplier<Long> supplier =
                    new Supplier<Long>() {
                      public Long get() {
                        // TODO Blaah! Initializing all the values to zero!? Would be better
                        // registering
                        //     dynamically the metrics
                        // TODO add grace period!!
                        return gitRepoMetricsCache.getMetrics().getOrDefault(name, 0L);
                      }
                    };

                gitRepoMetricsCache.metricMaker.newCallbackMetric(
                    name,
                    Long.class,
                    new Description(gitRepoMetric.getDescription())
                        .setRate()
                        .setUnit(gitRepoMetric.getUnit()),
                    supplier);
              });
        });
  }

  public static String getMetricName(String metricName, String projectName) {
    return String.format("%s_%s", metricName, projectName).toLowerCase(Locale.ROOT);
  }

  public boolean doCollectStats(String projectName) {
    return projects.stream().anyMatch(p -> p.equals(projectName));
  }
}
