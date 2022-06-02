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
import java.util.Collections;
import org.junit.Test;

public class GitRepoMetricsCacheTest {

  GitRepoMetricsCache gitRepoMetricsCacheModule;
  GitRepoMetricsConfig gitRepoMetricsConfig;
  FakeMetricMaker fakeMetricMaker;

  @Test
  public void shouldRegisterMetrics() {
    gitRepoMetricsConfig = Utils.getRpoConfig(Collections.singletonList("repo1"));
    fakeMetricMaker = new FakeMetricMaker();
    gitRepoMetricsCacheModule = new GitRepoMetricsCache(fakeMetricMaker, gitRepoMetricsConfig);
    GitRepoMetricsCache.initCache(gitRepoMetricsCacheModule);
    assertThat(fakeMetricMaker.callsCounter).isEqualTo(GitStats.availableMetrics().size());
  }

  @Test
  public void shouldCollectStatsWhenGracePeriodNotSet() {
    gitRepoMetricsConfig = Utils.getRpoConfig(Collections.singletonList("repo1"), "0m");
    gitRepoMetricsCacheModule =
        new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    String testMetric = "testMetric";
    gitRepoMetricsCacheModule.setMetrics(ImmutableMap.of("anyMetric", 0L), testMetric);

    assertThat(gitRepoMetricsCacheModule.doCollectStats(testMetric)).isTrue();
  }

  @Test
  public void shouldNotCollectStatsWhenGracePeriodIsNotExpired() {
    gitRepoMetricsConfig = Utils.getRpoConfig(Collections.singletonList("repo1"), "5m");
    gitRepoMetricsCacheModule =
        new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    String testMetric = "testMetric";
    gitRepoMetricsCacheModule.setMetrics(ImmutableMap.of("anyMetric", 0L), testMetric);

    assertThat(gitRepoMetricsCacheModule.doCollectStats(testMetric)).isFalse();
  }

  @Test
  public void shouldCollectStatsWhenGracePeriodIsExpired() throws InterruptedException {
    gitRepoMetricsConfig = Utils.getRpoConfig(Collections.singletonList("repo1"), "1s");
    gitRepoMetricsCacheModule =
        new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    String testMetric = "testMetric";
    gitRepoMetricsCacheModule.setMetrics(ImmutableMap.of("anyMetric", 0L), testMetric);

    assertThat(gitRepoMetricsCacheModule.doCollectStats(testMetric)).isFalse();

    // Make sure grace period is expired
    Thread.sleep(1000);

    assertThat(gitRepoMetricsCacheModule.doCollectStats(testMetric)).isTrue();
  }

  @Test
  public void shouldSetCollectionTime() {
    gitRepoMetricsConfig = Utils.getRpoConfig(Collections.singletonList("repo1"));
    gitRepoMetricsCacheModule =
        new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    long currentTimeStamp = System.currentTimeMillis();

    String testMetric = "testMetric";
    gitRepoMetricsCacheModule.setMetrics(ImmutableMap.of("anyMetric", 0L), testMetric);

    assertThat(gitRepoMetricsCacheModule.getCollectedAt().get(testMetric))
        .isAtLeast(currentTimeStamp);
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
