package com.googlesource.gerrit.plugins.gitrepometrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import org.junit.Ignore;

@Ignore
public class FakeMetricMaker extends DisabledMetricMaker {
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
