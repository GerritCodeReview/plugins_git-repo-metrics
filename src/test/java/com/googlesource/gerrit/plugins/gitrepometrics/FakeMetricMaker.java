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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.metrics.CallbackMetric;
import com.google.gerrit.metrics.CallbackMetric1;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.Field;

class FakeMetricMaker extends DisabledMetricMaker {
  private final ProjectlessMetricsTracker metricTracker;
  private final MetricRegistry metricRegistry;
  Integer callsCounter;

  FakeMetricMaker(MetricRegistry metricRegistry) {
    callsCounter = 0;
    this.metricRegistry = metricRegistry;
    this.metricTracker = new ProjectlessMetricsTracker("git-repo-metrics", metricRegistry);
  }

  @Override
  public <F1, V> CallbackMetric1<F1, V> newCallbackMetric(
      String name, Class<V> valueClass, Description desc, Field<F1> field1) {
    return new CallbackMetric1<F1, V>() {

      @Override
      public void set(F1 field1, V value) {
        callsCounter += 1;
        metricRegistry.register(
            String.format("%s/%s/%s/%s", "plugins", "git-repo-metrics", name, field1.toString()),
            new Meter());
      }

      @Override
      public void forceCreate(F1 field1) {}

      @Override
      public void remove() {}
    };
  }

  @Override
  public RegistrationHandle newTrigger(CallbackMetric<?> metric1, Runnable trigger) {
    trigger.run();
    return () -> {};
  }
}
