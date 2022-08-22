package com.googlesource.gerrit.plugins.gitrepometrics.collectors;

import com.google.gerrit.entities.Project;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jgit.internal.storage.file.FileRepository;

public interface StatsCollector {

  HashMap<String, Long> collect();

  StatsCollector create(FileRepository repository, Project project);

  String getStatName();

  List<GitRepoMetric> availableMetrics();
}
