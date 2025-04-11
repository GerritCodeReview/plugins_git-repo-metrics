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

import static com.google.common.truth.Truth.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class GitRepoMetricsCacheTest {
  GitRepoMetricsCache gitRepoMetricsCache;
  GitRepoMetricsConfig gitRepoMetricsConfig;
  FakeMetricMaker fakeMetricMaker;
  MetricRegistry metricRegistry;
  private ConfigSetupUtils configSetupUtils;
  private final String enabledRepo = "enabledRepo";

  private FakeMetricsCollector fakeStatsCollector;
  private DynamicSet<MetricsCollector> ds;

  @Before
  public void setupRepo() throws IOException {
    configSetupUtils = new ConfigSetupUtils(Collections.singletonList(enabledRepo));
    metricRegistry = new MetricRegistry();
    fakeMetricMaker = new FakeMetricMaker(metricRegistry);

    fakeStatsCollector = new FakeMetricsCollector();
    ds = new DynamicSet<MetricsCollector>();
    ds.add("git-repo-metrics", fakeStatsCollector);
  }

  @Test
  public void shouldRegisterMetrics() {
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, new MetricRegistry(), gitRepoMetricsConfig);

    gitRepoMetricsCache.setMetrics(getCollectedMetrics(), "anyRepo");

    assertThat(fakeMetricMaker.callsCounter).isEqualTo(1);
  }

  @Test
  public void shouldRegisterMetricsOnlyOnce() {
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, metricRegistry, gitRepoMetricsConfig);

    gitRepoMetricsCache.setMetrics(getCollectedMetrics(), "anyRepo");

    assertThat(fakeMetricMaker.callsCounter).isEqualTo(1);

    gitRepoMetricsCache.setMetrics(getCollectedMetrics(), "anyRepo");

    assertThat(fakeMetricMaker.callsCounter).isEqualTo(1);
  }

  @Test
  public void shouldCollectStatsForEnabledRepo() {
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();

    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, new MetricRegistry(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldCollectStatsForAllRepos() throws Exception {
    gitRepoMetricsConfig = new ConfigSetupUtils(List.of(), "0", true).getGitRepoMetricsConfig();

    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, new MetricRegistry(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCache.shouldCollectStats("new-repo")).isTrue();
  }

  @Test
  public void shouldNotCollectStatsForDisabledRepo() {
    String disabledRepo = "disabledRepo";
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, new MetricRegistry(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCache.shouldCollectStats(disabledRepo)).isFalse();
  }

  @Test
  public void shouldCollectStatsWhenGracePeriodNotSet() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo));
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, new MetricRegistry(), gitRepoMetricsConfig);

    gitRepoMetricsCache.setMetrics(getCollectedMetrics(), enabledRepo);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldSkipCollectionWhenGracePeriodIsNotExpired() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo), "5 m");
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, new MetricRegistry(), gitRepoMetricsConfig);

    gitRepoMetricsCache.setMetrics(getCollectedMetrics(), enabledRepo);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isFalse();
  }

  @Test
  public void shouldCollectStatsWhenGracePeriodIsExpired() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo), "1 s");
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            ds,
            fakeMetricMaker,
            metricRegistry,
            gitRepoMetricsConfig,
            Clock.fixed(
                Instant.now().minus(2, ChronoUnit.SECONDS), Clock.systemDefaultZone().getZone()));

    gitRepoMetricsCache.setMetrics(getCollectedMetrics(), enabledRepo);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldSetCollectionTime() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo));
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, metricRegistry, gitRepoMetricsConfig);

    long currentTimeStamp = System.currentTimeMillis();

    gitRepoMetricsCache.setMetrics(getCollectedMetrics(), enabledRepo);

    assertThat(gitRepoMetricsCache.getCollectedAt().get(enabledRepo)).isAtLeast(currentTimeStamp);
  }

  private HashMap<GitRepoMetric, Long> getCollectedMetrics() {
    return Maps.newHashMap(
        ImmutableMap.of(new GitRepoMetric("anyMetrics", "anyMetric description", "Count"), 1L));
  }
}
