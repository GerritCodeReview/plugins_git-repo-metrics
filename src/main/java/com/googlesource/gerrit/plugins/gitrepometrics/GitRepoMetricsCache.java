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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GitRepoMetricsCache {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private Map<String, Long> metrics;
  private final MetricMaker metricMaker;
  private final MetricRegistry metricRegistry;
  private final List<String> projects;
  private Map<String, Long> collectedAt;
  private final long gracePeriodMs;
  private ImmutableList<GitRepoMetric> metricsNames;
  private DynamicSet<MetricsCollector> collectors;

  private final Clock clock;

  @VisibleForTesting
  GitRepoMetricsCache(
      DynamicSet<MetricsCollector> collectors,
      MetricMaker metricMaker,
      MetricRegistry metricRegistry,
      GitRepoMetricsConfig config,
      Clock clock) {
    this.collectors = collectors;
    this.metricMaker = metricMaker;
    this.metricRegistry = metricRegistry;

    this.metricsNames =
        collectors.stream()
            .flatMap(c -> c.availableMetrics().stream())
            .collect(collectingAndThen(toList(), ImmutableList::copyOf));

    this.projects = config.getRepositoryNames();
    this.metrics = Maps.newHashMap();
    this.collectedAt = Maps.newHashMap();
    this.clock = clock;
    this.gracePeriodMs = config.getGracePeriodMs();
  }

  @Inject
  GitRepoMetricsCache(
      DynamicSet<MetricsCollector> collectors,
      MetricMaker metricMaker,
      MetricRegistry metricRegistry,
      GitRepoMetricsConfig config) {
    this(collectors, metricMaker, metricRegistry, config, Clock.systemDefaultZone());
  }

  public Map<String, Long> getMetrics() {
    return metrics;
  }

  public void setMetric(GitRepoMetric newMetric, Long value, String projectName) {
    this.collectedAt.put(projectName, clock.millis());
    metrics.put(getMetricName(newMetric.getName(), projectName), value);
  }

  public List<GitRepoMetric> getMetricsNames() {
    return metricsNames;
  }

  public DynamicSet<MetricsCollector> getCollectors() {
    return collectors;
  }

  @VisibleForTesting
  public Map<String, Long> getCollectedAt() {
    return collectedAt;
  }

  public void initCache() {
    metricsNames.forEach(
        gitRepoMetric -> {
          projects.forEach(
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
                        return getMetrics().getOrDefault(name, 0L);
                      }
                    };

                if (!metricRegistry
                    .getMetrics()
                    .containsKey(
                        GitRepoMetricsCache.getFullyQualifiedMetricName(
                            gitRepoMetric.getName(), projectName))) {
                  metricMaker.newCallbackMetric(
                      name,
                      Long.class,
                      new Description(gitRepoMetric.getDescription())
                          .setRate()
                          .setUnit(gitRepoMetric.getUnit()),
                      supplier);
                }
              });
        });
  }

  public static String getMetricName(String metricName, String projectName) {
    return String.format("%s_%s", metricName, projectName).toLowerCase(Locale.ROOT);
  }

  @VisibleForTesting
  static String getFullyQualifiedMetricName(String metricName, String projectName) {
    return String.format(
        "%s/%s/%s", "plugins", "git-repo-metrics", getMetricName(metricName, projectName));
  }

  public boolean shouldCollectStats(String projectName) {
    long lastCollectionTime = collectedAt.getOrDefault(projectName, 0L);
    long currentTimeMs = System.currentTimeMillis();
    boolean doCollectStats = lastCollectionTime + gracePeriodMs <= currentTimeMs;
    if (!doCollectStats) {
      logger.atFine().log(
          "Skip stats collection for %s (grace period: %d, last collection time: %d, current time: %d",
          projectName, gracePeriodMs, lastCollectionTime, currentTimeMs);
      return false;
    }

    return projects.stream().anyMatch(p -> p.equals(projectName));
  }
}
