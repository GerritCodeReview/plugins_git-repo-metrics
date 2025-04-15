// Copyright (C) 2025 The Android Open Source Project
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

import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectMetricsThrottler implements ProjectMetricsLimiter {
  private final GitRepoMetricsConfig repoMetricsConfig;

  @Inject
  ProjectMetricsThrottler(GitRepoMetricsConfig repoMetricsConfig) {
    this.repoMetricsConfig = repoMetricsConfig;
  }

  private ConcurrentHashMap<String, RateLimiter> projectsRateLimiters = new ConcurrentHashMap<>();

  @Override
  public void acquire(String projectName) {
    double rate = (double) 1000L / repoMetricsConfig.getGracePeriodMs();
    projectsRateLimiters.computeIfAbsent(projectName, (p) -> RateLimiter.create(rate)).acquire();
  }
}
