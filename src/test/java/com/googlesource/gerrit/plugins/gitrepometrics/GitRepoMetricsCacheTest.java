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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.testing.InMemoryRepositoryManager;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GitRepoMetricsCacheTest {
  GitRepoMetricsCache gitRepoMetricsCache;
  GitRepoMetricsConfig gitRepoMetricsConfig;
  FakeMetricMaker fakeMetricMaker;
  private ConfigSetupUtils configSetupUtils;
  private final String enabledRepo = "enabledRepo";

  private FakeMetricsCollector fakeStatsCollector;
  private DynamicSet<MetricsCollector> ds;

  private Repository enabledRepository;

  @Before
  public void setupRepo() throws IOException, GitAPIException {
    configSetupUtils = new ConfigSetupUtils(Collections.singletonList(enabledRepo));

    fakeStatsCollector = new FakeMetricsCollector();
    ds = new DynamicSet<>();
    ds.add("git-repo-metrics", fakeStatsCollector);


    enabledRepository = configSetupUtils.createRepository(enabledRepo);
  }

  @Test
  public void shouldRegisterMetrics() throws IOException {
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    fakeMetricMaker = new FakeMetricMaker();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(ds, fakeMetricMaker, new MetricRegistry(), gitRepoMetricsConfig);



    new UpdateGitMetricsTask(gitRepoMetricsCache, repoManager, enabledRepo).run();

    assertThat(fakeMetricMaker.callsCounter)
        .isEqualTo(fakeStatsCollector.availableMetrics().size());
  }

  @Test
  public void shouldCollectStatsForEnabledRepo() throws IOException {
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            ds, new FakeMetricMaker(), new MetricRegistry(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldNotCollectStatsForDisabledRepo() throws IOException {
    String disabledRepo = "disabledRepo";
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            ds, new FakeMetricMaker(), new MetricRegistry(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCache.shouldCollectStats(disabledRepo)).isFalse();
  }

//  @Test
//  public void shouldCollectStatsWhenGracePeriodNotSet() throws IOException {
//    ConfigSetupUtils configSetupUtils =
//        new ConfigSetupUtils(Collections.singletonList(enabledRepo));
//    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
//    gitRepoMetricsCache =
//        new GitRepoMetricsCache(
//            ds, new FakeMetricMaker(), new MetricRegistry(), gitRepoMetricsConfig);
//
//    gitRepoMetricsCache.setMetrics(
//        new HashMap<String, Long>() {
//          {
//            put("anyMetric", 0L);
//          }
//        },
//        enabledRepo);
//
//    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
//  }
//
//  @Test
//  public void shouldMergeMetricsFromDifferentRepositories() throws IOException {
//    final String enabledRepoA = "enabledRepoA";
//    final String enabledRepoB = "enabledRepoB";
//    ConfigSetupUtils configSetupUtils =
//        new ConfigSetupUtils(Arrays.asList(enabledRepoA, enabledRepoB));
//    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
//    gitRepoMetricsCache =
//        new GitRepoMetricsCache(
//            ds, new FakeMetricMaker(), new MetricRegistry(), gitRepoMetricsConfig);
//
//    gitRepoMetricsCache.setMetrics(
//        new HashMap<String, Long>() {
//          {
//            put("anyMetricA", 0L);
//          }
//        },
//        enabledRepoA);
//    gitRepoMetricsCache.setMetrics(
//        new HashMap<String, Long>() {
//          {
//            put("anyMetricB", 0L);
//          }
//        },
//        enabledRepoB);
//
//    assertThat(gitRepoMetricsCache.getMetrics().size()).isEqualTo(2);
//    assertThat(
//            gitRepoMetricsCache
//                .getMetrics()
//                .keySet()
//                .containsAll(Arrays.asList("anyMetricA", "anyMetricB")))
//        .isTrue();
//  }
//
//  @Test
//  public void shouldSkipCollectionWhenGracePeriodIsNotExpired() throws IOException {
//    ConfigSetupUtils configSetupUtils =
//        new ConfigSetupUtils(Collections.singletonList(enabledRepo), "5 m");
//    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
//    gitRepoMetricsCache =
//        new GitRepoMetricsCache(
//            ds, new FakeMetricMaker(), new MetricRegistry(), gitRepoMetricsConfig);
//
//    gitRepoMetricsCache.setMetrics(
//        new HashMap<String, Long>() {
//          {
//            put("anyMetric", 0L);
//          }
//        },
//        enabledRepo);
//
//    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isFalse();
//  }
//
//  @Test
//  public void shouldCollectStatsWhenGracePeriodIsExpired() throws IOException {
//    ConfigSetupUtils configSetupUtils =
//        new ConfigSetupUtils(Collections.singletonList(enabledRepo), "1 s");
//    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
//    gitRepoMetricsCache =
//        new GitRepoMetricsCache(
//            ds,
//            new FakeMetricMaker(),
//            new MetricRegistry(),
//            gitRepoMetricsConfig,
//            Clock.fixed(
//                Instant.now().minus(2, ChronoUnit.SECONDS), Clock.systemDefaultZone().getZone()));
//
//    gitRepoMetricsCache.setMetrics(
//        new HashMap<String, Long>() {
//          {
//            put("anyMetric", 0L);
//          }
//        },
//        enabledRepo);
//
//    assertThat(gitRepoMetricsCache.shouldCollectStats(enabledRepo)).isTrue();
//  }
//
//  @Test
//  public void shouldSetCollectionTime() throws IOException {
//    ConfigSetupUtils configSetupUtils =
//        new ConfigSetupUtils(Collections.singletonList(enabledRepo));
//    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
//    gitRepoMetricsCache =
//        new GitRepoMetricsCache(
//            ds, new FakeMetricMaker(), new MetricRegistry(), gitRepoMetricsConfig);
//
//    long currentTimeStamp = System.currentTimeMillis();
//
//    gitRepoMetricsCache.setMetrics(
//        new HashMap<String, Long>() {
//          {
//            put("anyMetric", 0L);
//          }
//        },
//        enabledRepo);
//
//    assertThat(gitRepoMetricsCache.getCollectedAt().get(enabledRepo)).isAtLeast(currentTimeStamp);
//  }

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

  private class FakeMetric implements Metric {
    FakeMetric() {}
  }
}
