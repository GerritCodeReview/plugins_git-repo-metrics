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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.googlesource.gerrit.plugins.gitrepometrics.GitRepoMetricsCache;
import org.eclipse.jgit.internal.storage.file.FileRepository;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FSStats implements MetricsCollector {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public static String numberOfKeepFiles = "numberOfKeepFiles";
    public static String numberOfEmptyDirectories = "numberOfEmptyDirectories";
    public static String numberOfDirectories = "numberOfDirectories";
    public static String numberOfFiles = "numberOfFiles";

    @Override
    public HashMap <String, Long> collect(FileRepository repository, Project project) {
        HashMap<String, Long> metrics = new HashMap<>();

        File packDirectory = new File(repository.getObjectsDirectory(), "pack");
        File[] keepFiles = packDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("keep");
            }
        });

        putMetric(project, metrics, numberOfKeepFiles, keepFiles != null ? keepFiles.length : 0);

        try {
            AtomicInteger filesCount = new AtomicInteger(0);
            AtomicInteger directoryCount = new AtomicInteger(0);
            AtomicInteger emptyDirectoryCount = new AtomicInteger(0);
            Files.walk(repository.getObjectsDirectory().toPath())
                    .parallel().forEach(path -> {
                        if (path.toFile().isFile()) {
                            filesCount.set(filesCount.get() + 1);
                        }
                        if (path.toFile().isDirectory()) {
                            directoryCount.set(directoryCount.get() + 1);
                            if (Objects.requireNonNull(path.toFile().listFiles()).length == 0) {
                                emptyDirectoryCount.set(emptyDirectoryCount.get() + 1);
                            }
                        }
                    });
            putMetric(project, metrics, numberOfEmptyDirectories, emptyDirectoryCount.get());
            putMetric(project, metrics, numberOfDirectories, directoryCount.get());
            putMetric(project, metrics, numberOfFiles, filesCount.get());
        } catch (IOException e ) {
            logger.atSevere().withCause(e).log("Can't open object directory for project " + project.getName());
        }

        return metrics;
    }

    private void putMetric(
            Project project, HashMap<String, Long> metrics, String metricName, long value) {
        metrics.put(GitRepoMetricsCache.getMetricName(metricName, project.getName()), value);
    }

    @Override
    public String getMetricsCollectorName() {
        return "filesystem-statistics";
    }

    @Override
    public List <GitRepoMetric> availableMetrics() {
        return Arrays.asList(
                new GitRepoMetric(numberOfKeepFiles, "Number of keep files on filesystem", "Count"),
                new GitRepoMetric(numberOfEmptyDirectories, "Number of empty directories on filesystem", "Count"),
                new GitRepoMetric(numberOfDirectories, "Number of directories on filesystem", "Count"),
                new GitRepoMetric(numberOfFiles, "Number of directories on filesystem", "Count")
        );
    }
}
