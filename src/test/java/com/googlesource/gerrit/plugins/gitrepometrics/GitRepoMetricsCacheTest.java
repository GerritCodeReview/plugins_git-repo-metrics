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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStats;
import java.util.Collections;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class GitRepoMetricsCacheTest {

  PluginConfigFactory pluginConfigFactory = mock(PluginConfigFactory.class);

  GitRepoMetricsCache gitRepoMetricsCacheModule;
  GitRepoMetricsConfig gitRepoMetricsConfig;
  FakeMetricMaker fakeMetricMaker;

  @Test
  public void shouldRegisterMetrics() {
    Config c = new Config();
    c.setStringList(
        GitRepoMetricsConfig.PLUGIN_NAME, null, "project", Collections.singletonList("repo1"));
    doReturn(c).when(pluginConfigFactory).getGlobalPluginConfig(any());
    gitRepoMetricsConfig =
        new GitRepoMetricsConfig(pluginConfigFactory, GitRepoMetricsConfig.PLUGIN_NAME);
    fakeMetricMaker = new FakeMetricMaker();
    gitRepoMetricsCacheModule = new GitRepoMetricsCache(fakeMetricMaker, gitRepoMetricsConfig);
    gitRepoMetricsCacheModule.initCache();
    assertThat(fakeMetricMaker.callsCounter).isEqualTo(GitStats.availableMetrics().size());
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
