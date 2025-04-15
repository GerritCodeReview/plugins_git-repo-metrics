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

import com.google.inject.ImplementedBy;

/**
 * A limiter interface for controlling the collection of Git repository metrics per project.
 *
 * <p>Implementations of this interface can apply throttling policy to regulate how often metrics
 * collection tasks are run concurrently for a given project.
 *
 * <p>By default, this interface is implemented by {@link ProjectMetricsUnlimited}, which imposes no
 * restrictions.
 */
@ImplementedBy(ProjectMetricsUnlimited.class)
public interface ProjectMetricsLimiter {
  /**
   * Acquires permission to collect metrics for the given project.
   *
   * <p>This method may block, if the metrics collection for the specified project is currently
   * throttled.
   *
   * @param projectName the name of the project for which metrics collection is being triggered
   */
  void acquire(String projectName);
}
