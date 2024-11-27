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
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;

class FakeMetricMaker extends DisabledMetricMaker {
  Integer callsCounter;
  private MetricRegistry metricRegistry;

  FakeMetricMaker(MetricRegistry metricRegistry) {
    callsCounter = 0;
    this.metricRegistry = metricRegistry;
  }

  @Override
  public <V> CallbackMetric0<V> newCallbackMetric(
      String name, Class<V> valueClass, Description desc) {

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
}
