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
import java.util.Optional;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Supplier;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;

class FakeMetricMaker extends DisabledMetricMaker {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  Integer callsCounter;
  private MetricRegistry metricRegistry;
  HashMap<String, Supplier<?>> actionMap;

  @SuppressWarnings({"rawtypes", "unchecked"})
  FakeMetricMaker(MetricRegistry metricRegistry) {
    this.callsCounter = 0;
    this.metricRegistry = metricRegistry;
    this.actionMap = new HashMap();
  }

  @SuppressWarnings("unused")
  @Override
  public <V> CallbackMetric0<V> newCallbackMetric(
      String name, Class<V> valueClass, Description desc) {

    logger.atSevere().log("Registering metric 1: %s", name);
    callsCounter += 1;
    metricRegistry.register(
        String.format("%s/%s/%s", "plugins", "git-repo-metrics", name), new Meter());
    return new CallbackMetric0<V>() {

      @Override
      public void set(V value) {}

      @Override
      public void remove() {}
    };
  }

  @Override
  public <V> RegistrationHandle newCallbackMetric(
      String name, Class<V> valueClass, Description desc, Supplier<V> trigger) {
    logger.atSevere().log("Registering metric 2: %s", name);
    callsCounter += 1;

    String metricName = String.format("%s/%s/%s", "plugins", "git-repo-metrics", name);

    metricRegistry.register(metricName, new Meter());

    actionMap.put(metricName, trigger);

    return null;
  }

  @SuppressWarnings("rawtypes")
  public Optional<Supplier> getValueForMetric(String metric) {
    if (actionMap.containsKey(metric)) return Optional.of(actionMap.get(metric));

    return Optional.empty();
  }
}
