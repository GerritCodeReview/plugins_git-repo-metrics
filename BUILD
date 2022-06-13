load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

DELEGATE_REPOSITORY_UNWRAPPER_SRCS = ["src/main/java/com/google/gerrit/server/git/DelegateRepositoryUnwrapper.java"]

gerrit_plugin(
    name = "git-repo-metrics",
    srcs = glob(
        ["src/main/java/**/*.java"],
        exclude = DELEGATE_REPOSITORY_UNWRAPPER_SRCS,
    ),
    manifest_entries = [
        "Gerrit-PluginName: git-repo-metrics",
        "Gerrit-Module: com.googlesource.gerrit.plugins.gitrepometrics.Module",
        "Implementation-Title: git-repo-metrics plugin",
        "Implementation-URL: https://review.gerrithub.io/admin/repos/GerritForge/git-repo-metrics",
        "Implementation-Vendor: GerritForge",
    ],
    resources = glob(
        ["src/main/resources/**/*"],
    ),
    deps = [
        ":delegaterepositoryunwrapper-neverlink",
    ],
)

java_library(
    name = "delegaterepositoryunwrapper",
    srcs = DELEGATE_REPOSITORY_UNWRAPPER_SRCS,
    deps = PLUGIN_DEPS,
)

java_library(
    name = "delegaterepositoryunwrapper-neverlink",
    srcs = DELEGATE_REPOSITORY_UNWRAPPER_SRCS,
    neverlink = True,
    deps = PLUGIN_DEPS,
)

junit_tests(
    name = "git-repo-metrics_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    resources = glob(["src/test/resources/**/*"]),
    tags = [
        "git-repo-metrics",
    ],
    deps = [
        ":git-repo-metrics__plugin_test_deps",
    ],
)

java_library(
    name = "git-repo-metrics__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":git-repo-metrics__plugin",
    ],
)

java_library(
    name = "git-repo-metrics__plugin_deps",
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS,
)
