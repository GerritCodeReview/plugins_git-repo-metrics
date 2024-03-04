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

import com.google.common.base.Supplier;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.Description.Units;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.server.restapi.project.ListProjects;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RepoCountMetricRunner implements LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final Provider<ListProjects> listProjectsProvider;
  private final MetricMaker metricMaker;

  @Inject
  RepoCountMetricRunner(Provider<ListProjects> listProjectsProvider, MetricMaker metricMaker) {
    this.listProjectsProvider = listProjectsProvider;
    this.metricMaker = metricMaker;
  }

  @Override
  public void start() {
    logger.atInfo().log("Registering metric numberofproject");

    metricMaker.newCallbackMetric(
        "numberofprojects",
        Long.class,
        new Description("Number of existing projects.").setGauge().setUnit(Units.BYTES),
        new Supplier<Long>() {
          @Override
          public Long get() {
            ListProjects listProjects = listProjectsProvider.get();
            listProjects.setStart(0);
            listProjects.setShowDescription(false);

            try {
              return Long.valueOf(listProjects.apply().size());
            } catch (Exception e) {
              logger.atSevere().withCause(e).log("Error getting repo count");
            }

            return 0L;
          }
        });
  }

  @Override
  public void stop() {}
}
