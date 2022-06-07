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

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStats;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class GitRepoMetricsCacheTest {
  GitRepoMetricsCache gitRepoMetricsCache;
  GitRepoMetricsConfig gitRepoMetricsConfig;
  FakeMetricMaker fakeMetricMaker;
  private ConfigSetupUtils configSetupUtils;
  private final String enabledRepo = "enabledRepo";

  @Before
  public void setupRepo() throws IOException {
    configSetupUtils = new ConfigSetupUtils(Collections.singletonList(enabledRepo));
  }

  @Test
  public void shouldRegisterMetrics() throws IOException {
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    fakeMetricMaker = new FakeMetricMaker();
    gitRepoMetricsCache = new GitRepoMetricsCache(fakeMetricMaker, gitRepoMetricsConfig);
    gitRepoMetricsCache.initCache();
    assertThat(fakeMetricMaker.callsCounter).isEqualTo(GitStats.availableMetrics().size());
  }

  @Test
  public void shouldCollectStatsForEnabledRepo() throws IOException {
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache = new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldNotCollectStatsForDisabledRepo() throws IOException {
    String disabledRepo = "disabledRepo";
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache = new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCache.shouldCollectStats(disabledRepo)).isFalse();
  }

  @Test
  public void shouldCollectStatsWhenGracePeriodNotSet() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo));
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache = new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    gitRepoMetricsCache.setMetrics(ImmutableMap.of("anyMetric", 0L), enabledRepo);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldSkipCollectionWhenGracePeriodIsNotExpired() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo), "5 m");
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache = new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    gitRepoMetricsCache.setMetrics(ImmutableMap.of("anyMetric", 0L), enabledRepo);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isFalse();
  }

  @Test
  public void shouldCollectStatsWhenGracePeriodIsExpired() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo), "1 s");
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            new FakeMetricMaker(),
            gitRepoMetricsConfig,
            Clock.fixed(
                Instant.now().minus(2, ChronoUnit.SECONDS), Clock.systemDefaultZone().getZone()));

    gitRepoMetricsCache.setMetrics(ImmutableMap.of("anyMetric", 0L), enabledRepo);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldSetCollectionTime() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Collections.singletonList(enabledRepo));
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache = new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    long currentTimeStamp = System.currentTimeMillis();

    gitRepoMetricsCache.setMetrics(ImmutableMap.of("anyMetric", 0L), enabledRepo);

    assertThat(gitRepoMetricsCache.getCollectedAt().get(enabledRepo)).isAtLeast(currentTimeStamp);
  }

  private class FakeMetricMaker extends DisabledMetricMaker {
    Integer callsCounter;

    FakeMetricMaker() {
      callsCounter = 0;
    }

    @Override
    public <V> CallbackMetric0<V> newCallbackMetric(
        String name, Class<V> valueClass, Description desc) {

      callsCounter += 1;
      return new CallbackMetric0<V>() {

        @Override
        public void set(V value) {}

        @Override
        public void remove() {}
      };
    }
  }
}
