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

import org.apache.commons.codec.digest.DigestUtils;

public enum GitBackend {
  GERRIT {
    @Override
    public String repoPath(String projectName) {
      return projectName;
    }
  },

  GITLAB {
    @Override
    public String repoPath(String projectName) {
      String sha256OfProjectName = DigestUtils.sha256Hex(projectName);
      return String.format(
          "%s/%s/%s",
          sha256OfProjectName.substring(0, 2),
          sha256OfProjectName.substring(2, 4),
          sha256OfProjectName);
    }
  };

  abstract String repoPath(String projectName);
}
