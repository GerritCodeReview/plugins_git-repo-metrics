# Plugin to collect Git repository metrics

This plugin allows a systematic collection of repository metrics for multiple Git SCM systems.
Metrics are updated upon a `ref-update` receive.

## How to build

Clone or link this plugin to the plugins directory of Gerrit's source tree, and then run bazel build
on the plugin's directory.

Example:

```
git clone --recursive https://gerrit.googlesource.com/gerrit
git clone https://gerrit.googlesource.com/plugins/git-repo-metrics
pushd gerrit/plugins && ln -s ../../git-repo-metrics . && popd
cd gerrit && bazel build plugins/git-repo-metrics
```

The output plugin jar is created in:

```
bazel-genfiles/plugins/git-repo-metrics/git-repo-metrics.jar
```

## How to install with Gerrit

Copy the git-repo-metrics.jar into the Gerrit's /plugins directory and wait for the plugin to be automatically
loaded.

## How to install with a different SCM

This plugin can also work with Git repositories hosted by other Git based SCM tools,
however the metrics are still expose via Gerrit, so a dedicated Gerrit instance running alongside
the current SCM tool is still required.
So to make this plugin work with other Git SCM tools, a Gerrit installation needs to be set-up and
the `basePath` needs to be set to the git data directory of the tool of choice.
You will also need to set `gracePeriod` and `forceCollection`, as when using a different SCM tool
than Gerrit the usual hooks aren't triggered.
Also, a configuration option will need to be specified to indicate which Backend is being used.
Currently supported backend, other than GERRIT are:
- GITLAB

## Configuration

More information about the plugin configuration can be found in the [config.md](src/resources/Documentation/config.md)
file.
