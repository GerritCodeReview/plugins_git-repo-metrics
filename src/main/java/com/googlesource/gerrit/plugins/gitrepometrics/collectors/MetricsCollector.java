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

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.function.Consumer;
import org.eclipse.jgit.internal.storage.file.FileRepository;

/** This interface is meant to be implemented by Git repository metrics collectors. * */
public interface MetricsCollector {

  /**
   * Collect metrics from a {@link FileRepository}.
   *
   * @param projectName to collect metrics for
   * @param repository {@link FileRepository} to collect metrics from
   * @param populateMetrics callback to populate the collected metrics
   */
  void collect(
      FileRepository repository,
      String projectName,
      Consumer<HashMap<GitRepoMetric, Long>> populateMetrics);

  /**
   * Returns the name of the metric collector.
   *
   * @return {@code String} with the metric collector name
   */
  String getMetricsCollectorName();

  /**
   * Returns the list of available metrics provided by the collector.
   *
   * @return {@code ImmutableList<GitRepoMetric>} with the {@link GitRepoMetric} provided by the
   *     metric collector implementation.
   */
  ImmutableList<GitRepoMetric> availableMetrics();
}
