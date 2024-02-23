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

import java.util.HashMap;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GenericMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;

class FakeGenericMetricsCollector implements GenericMetricsCollector {
  private final String prefix;
  private final GitRepoMetric fakeMetric1;
  private final GitRepoMetric fakeMetric2;

  @Override
  public void collect(
      Consumer<HashMap<GitRepoMetric, Long>> populateMetrics) {
     populateMetrics.accept(
            Maps.newHashMap(ImmutableMap.of(fakeMetric1, 1L, fakeMetric2, 2L)));
  }

  @Override
  public String getMetricsCollectorName() {
    return prefix + "-fake-metrics-collector";
  }

  @Override
  public ImmutableList<GitRepoMetric> availableMetrics() {
    return ImmutableList.of(fakeMetric1, fakeMetric2);
  }

  protected FakeGenericMetricsCollector(String prefix) {
    this.prefix = prefix;
    this.fakeMetric1 = new GitRepoMetric(prefix + "-fake-metric-1", "Fake metric 1", "Count");
    this.fakeMetric2 = new GitRepoMetric(prefix + "-fake-metric-2", "Fake metric 2", "Count");
  }

  protected FakeGenericMetricsCollector() {
    this("defaultPrefix");
  }
}
