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

import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStats;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class GitRepoMetricsCacheTest {
  GitRepoMetricsCache gitRepoMetricsCacheModule;
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
    gitRepoMetricsCacheModule = new GitRepoMetricsCache(fakeMetricMaker, gitRepoMetricsConfig);
    gitRepoMetricsCacheModule.initCache();
    assertThat(fakeMetricMaker.callsCounter).isEqualTo(GitStats.availableMetrics().size());
  }

  @Test
  public void shouldCollectStatsForEnabledRepo() throws IOException {
    String enabledRepo = "enabledRepo";
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCacheModule =
        new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCacheModule.doCollectStats(enabledRepo)).isTrue();
  }

  @Test
  public void shouldNotCollectStatsForDisabledRepo() throws IOException {
    String disabledRepo = "disabledRepo";
    gitRepoMetricsConfig = configSetupUtils.getGitRepoMetricsConfig();
    gitRepoMetricsCacheModule =
        new GitRepoMetricsCache(new FakeMetricMaker(), gitRepoMetricsConfig);

    assertThat(gitRepoMetricsCacheModule.doCollectStats(disabledRepo)).isFalse();
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
