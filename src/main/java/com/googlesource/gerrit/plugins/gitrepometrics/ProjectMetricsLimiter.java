package com.googlesource.gerrit.plugins.gitrepometrics;

import com.google.inject.ImplementedBy;

@ImplementedBy(ProjectMetricsUlimited.class)
public interface ProjectMetricsLimiter {
	void acquire(String projectName);
}
