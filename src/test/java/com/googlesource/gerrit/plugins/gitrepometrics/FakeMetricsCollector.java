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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gerrit.entities.Project;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.junit.Ignore;

@Ignore
public class FakeMetricsCollector implements MetricsCollector {
  @Override
  public HashMap<String, Long> collect(FileRepository repository, Project project) {
    return Maps.newHashMap(ImmutableMap.of("fake-metrics-1", 1L, "fake-metrics-2", 2L));
  }

  @Override
  public String getMetricsCollectorName() {
    return "fake-metrics-collector";
  }

  @Override
  public List<GitRepoMetric> availableMetrics() {
    return Arrays.asList(
        new GitRepoMetric("fake-metric-1", "Fake metric 1", "Count"),
        new GitRepoMetric("fake-metric-2", "Fake metric 2", "Count"));
  }

  protected FakeMetricsCollector() {}
}