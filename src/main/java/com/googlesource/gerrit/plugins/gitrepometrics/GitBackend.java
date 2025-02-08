package com.googlesource.gerrit.plugins.gitrepometrics;

import org.apache.commons.codec.digest.DigestUtils;

public enum GitBackend {
  Gerrit {
    @Override
    public String repoPath(String projectName) {
      return projectName;
    }
  },

  GitLab {
    @Override
    public String repoPath(String projectName) {
      String sha256OfProjectName = DigestUtils.sha256Hex(projectName);
      return String.format("%s/%s/%s.git",
          sha256OfProjectName.substring(0, 2),
          sha256OfProjectName.substring(2, 4),
          sha256OfProjectName);
    }
  };

  abstract String repoPath(String projectName);

}
