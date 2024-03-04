// Copyright (C) 2024 The Android Open Source Project
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

import javax.inject.Inject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Singleton;

@Singleton
public class RepoCountMetricRegister implements LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  protected static final String REPO_COUNT_METRIC_NAME = "numberofprojects";
  private final MetricMaker metricMaker;
  private final ProjectCache projectCache;

  @VisibleForTesting
  @Inject
  RepoCountMetricRegister(ProjectCache projectCache, MetricMaker metricMaker) {
    this.metricMaker = metricMaker;
    this.projectCache = projectCache;
  }

  @Override
  public void start() {
    logger.atInfo().log("Registering metric " + REPO_COUNT_METRIC_NAME);

    metricMaker.newCallbackMetric(
        REPO_COUNT_METRIC_NAME,
        Long.class,
        new Description("Number of existing projects.").setGauge().setUnit("Count"),
        new Supplier<Long>() {
          @Override
          public Long get() {
            return (long) projectCache.all().size();
          }
        });
  }

  @Override
  public void stop() {}
}