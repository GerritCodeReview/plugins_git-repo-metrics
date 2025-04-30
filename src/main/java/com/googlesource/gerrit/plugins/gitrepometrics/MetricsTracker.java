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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;
import com.google.common.base.Splitter;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class MetricsTracker implements MetricRegistryListener {
  private final Set<String> projectLessMetricNames = ConcurrentHashMap.newKeySet();
  private final String metricsPrefix;

  @Inject
  MetricsTracker(@PluginName String pluginName, MetricRegistry metricRegistry) {
    this.metricsPrefix = "plugins/" + pluginName.toLowerCase(Locale.ROOT);
    metricRegistry.addListener(this);
  }

  @Override
  public void onGaugeAdded(String name, Gauge<?> gauge) {
    addProjectLessMetricName(name);
  }

  @Override
  public void onGaugeRemoved(String name) {
    removeProjectLessMetricName(name);
  }

  @Override
  public void onCounterAdded(String name, Counter counter) {
    addProjectLessMetricName(name);
  }

  @Override
  public void onCounterRemoved(String name) {
    removeProjectLessMetricName(name);
  }

  @Override
  public void onHistogramAdded(String name, Histogram histogram) {
    addProjectLessMetricName(name);
  }

  @Override
  public void onHistogramRemoved(String name) {
    removeProjectLessMetricName(name);
  }

  @Override
  public void onMeterAdded(String name, Meter meter) {
    addProjectLessMetricName(name);
  }

  @Override
  public void onMeterRemoved(String name) {
    removeProjectLessMetricName(name);
  }

  @Override
  public void onTimerAdded(String name, Timer timer) {
    addProjectLessMetricName(name);
  }

  @Override
  public void onTimerRemoved(String name) {
    removeProjectLessMetricName(name);
  }

  boolean metricExists(String metricName) {
    return projectLessMetricNames.contains(metricName.toLowerCase(Locale.ROOT));
  }

  private void addProjectLessMetricName(String name) {
    if (name.toLowerCase(Locale.ROOT).startsWith(metricsPrefix)) {
      projectLessMetricNames.add(dropProjectName(name));
    }
  }

  private void removeProjectLessMetricName(String name) {
    if (name.toLowerCase(Locale.ROOT).startsWith(metricsPrefix)) {
      projectLessMetricNames.remove(dropProjectName(name));
    }
  }

  private String dropProjectName(String fullMetricName) {
    List<String> metricParts =
        Splitter.on('/')
            .trimResults()
            .omitEmptyStrings()
            .splitToList(fullMetricName.toLowerCase(Locale.ROOT));
    return metricParts.get(metricParts.size() - 2);
  }
}
