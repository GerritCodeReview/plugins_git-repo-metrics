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

package com.googlesource.gerrit.plugins.gitrepometrics.collectors;

import com.google.gerrit.entities.Project;
import com.googlesource.gerrit.plugins.gitrepometrics.GitRepoMetricsCache;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jgit.internal.storage.file.FileRepository;

public interface MetricsCollector {

  HashMap<String, Long> collect(FileRepository repository, Project project);

  String getMetricsCollectorName();

  List<GitRepoMetric> availableMetrics();

  default void putMetric(
      Project project, HashMap<String, Long> metrics, String metricName, long value) {
    metrics.put(GitRepoMetricsCache.getMetricName(metricName, project.getName()), value);
  }
}
