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

package com.googlesource.gerrit.plugins.gitrepometrics.collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jgit.internal.storage.file.FileRepository;

public class FSStats implements MetricsCollector {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static String numberOfKeepFiles = "numberOfKeepFiles";
  public static String numberOfEmptyDirectories = "numberOfEmptyDirectories";
  public static String numberOfDirectories = "numberOfDirectories";
  public static String numberOfFiles = "numberOfFiles";

  @VisibleForTesting
  public FSStats() {}

  @Override
  public HashMap<String, Long> collect(FileRepository repository, Project project) {
    HashMap<String, Long> metrics = new HashMap<>();

    putMetric(project, metrics, numberOfKeepFiles, keepFilesCount(repository));

    HashMap<String, AtomicInteger> partialMetrics =
        filesAndDirectoriesCount(repository, project, metrics);
    putMetric(
        project,
        metrics,
        numberOfEmptyDirectories,
        partialMetrics.get(numberOfEmptyDirectories).longValue());
    putMetric(
        project, metrics, numberOfDirectories, partialMetrics.get(numberOfDirectories).longValue());
    putMetric(project, metrics, numberOfFiles, partialMetrics.get(numberOfFiles).longValue());

    return metrics;
  }

  private HashMap<String, AtomicInteger> filesAndDirectoriesCount(
      FileRepository repository, Project project, HashMap<String, Long> metrics) {
    HashMap<String, AtomicInteger> counter =
        new HashMap<String, AtomicInteger>() {
          {
            put(numberOfFiles, new AtomicInteger(0));
            put(numberOfDirectories, new AtomicInteger(0));
            put(numberOfEmptyDirectories, new AtomicInteger(0));
          }
        };
    try {

      Files.walk(repository.getObjectsDirectory().toPath())
          .parallel()
          .forEach(
              path -> {
                if (path.toFile().isFile()) {
                  counter.get(numberOfFiles).set(counter.get(numberOfFiles).get() + 1);
                }
                if (path.toFile().isDirectory()) {
                  counter.get(numberOfDirectories).set(counter.get(numberOfDirectories).get() + 1);
                  if (Objects.requireNonNull(path.toFile().listFiles()).length == 0) {
                    counter
                        .get(numberOfEmptyDirectories)
                        .set(counter.get(numberOfEmptyDirectories).get() + 1);
                  }
                }
              });
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Can't open object directory for project " + project.getName());
    }

    return counter;
  }

  private int keepFilesCount(FileRepository repository) {
    File packDirectory = new File(repository.getObjectsDirectory(), "pack");
    File[] keepFiles =
        packDirectory.listFiles(
            new FilenameFilter() {
              public boolean accept(File dir, String name) {
                return name.endsWith("keep");
              }
            });
    return keepFiles != null ? keepFiles.length : 0;
  }

  @Override
  public String getMetricsCollectorName() {
    return "filesystem-statistics";
  }

  @Override
  public List<GitRepoMetric> availableMetrics() {
    return Arrays.asList(
        new GitRepoMetric(numberOfKeepFiles, "Number of keep files on filesystem", "Count"),
        new GitRepoMetric(
            numberOfEmptyDirectories, "Number of empty directories on filesystem", "Count"),
        new GitRepoMetric(numberOfDirectories, "Number of directories on filesystem", "Count"),
        new GitRepoMetric(numberOfFiles, "Number of directories on filesystem", "Count"));
  }
}
