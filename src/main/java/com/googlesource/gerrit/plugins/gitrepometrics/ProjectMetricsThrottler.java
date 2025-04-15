package com.googlesource.gerrit.plugins.gitrepometrics;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;

public class ProjectMetricsThrottler implements ProjectMetricsLimiter {
	private final GitRepoMetricsConfig repoMetricsConfig;

	ProjectMetricsThrottler(GitRepoMetricsConfig repoMetricsConfig) {
		this.repoMetricsConfig = repoMetricsConfig;
	}

	private ConcurrentHashMap<String, RateLimiter> projectsRateLimiters = new ConcurrentHashMap<>();

	@Override
	public void acquire(String projectName) {
		projectsRateLimiters.computeIfAbsent(projectName, (p) -> RateLimiter.create(1000 / repoMetricsConfig.getGracePeriodMs())).acquire();
	}
}
