# Doop - Framework for Java Pointer and Taint Analysis (using P/Taint)

This document contains instructions for invoking the main driver of Doop. 
* For an introduction to Datalog, please consult [Datalog-101](docs/datalog-101.md). 
* For a more detailed tutorial on using the results of Doop analyses, please consult [Doop-101](docs/doop-101.md). 
* For an introduction to pointer analysis using Datalog, you can read a [research-level tutorial](http://yanniss.github.io/points-to-tutorial15.pdf).

## Edits (cmpared to doop-mirror repo)
* doop-mirror repo uses older souffle version; added .pragma "legacy" to start of souffle files
* Use --legacy option in souffle command to compile legacy version
* Groovy scripts updated to accomodate necessary changes

## Getting Started

For trouble-free configuration:

* The `DOOP_PLATFORMS_LIB` environment variable could point to your PLATFORM lib directory (optional, see below).
* The `DOOP_OUT` environment variable could point to the output files directory (optional, defaults to `out`).
* The `DOOP_CACHE` environment variable could point to the cached facts directory (optional, defaults to `cache`).
* The `DOOP_LOG` environment variable could point to a log output directory (optional, defaults to `build/logs`).
* The `LOGICBLOX_HOME` environment variable should point to the `logicblox` directory of the engine, if you want to use LogicBlox.

Support:

(a) Email: there is currently no mailing list, but there is an alias for support questions: doop-support@googlegroups.com

(b) [Discord server](https://discord.gg/4q7rj5s)

## Running Doop

Doop only supports invocations from its home directory. The main options when running Doop are the analysis and the jar(s) options. For example, for a context-insensitive analysis on a jar file we issue:

    $ ./doop -a context-insensitive -i com.example.some.jar

### Common command line options
To see the list of available options (and valid argument values in certain cases), issue:

    $ ./doop -h

The options will be also shown if you run Doop without any arguments.

The major command line options are the following:

#### Analysis (-a, --analysis)
Mandatory. The name of the analysis to run.

Example:

    $ ./doop -a context-insensitive

#### Input files  (-i, --input-file)
Mandatory. The input file(s) to analyse.

The inputs option accepts multiple values and/or can be repeated multiple times.

The value of the input file can be specified in the following manners:

* provide the relative or absolute path to a local input file.
* provide the URL of a remote input file.
* provide the relative or absolute path to a local directory and all its \*.jar files will be included.
* provide a maven-style expression to indicate a Jar file from the Maven central repository.

Example:

```
#!bash
$ ./doop -i ./lib/asm-debug-all-4.1.jar      [local file]
		 -i org.apache.ivy:ivy:2.3.0         [maven descriptor]
		 -i ./lib                            [local directory]
		 -i http://www.example.com/some.jar  [remote file]
		 -i one.jar other.jar                [multiple files separated with a space]
```

#### PLATFORM (--platform)
Optional --- default: java_8. The platform to use for the analysis. The possible Java options are java_N where N is the java version (3, 4, 5, 6, 7, 8 etc.). The android options are android_N_V where N is the Android version (20, 21, 22, 23, 24, 25 etc.) and V is the variant ("stubs" for the Android SDK libraries or "fulljars" for custom built platforms).

Example:

    $ ./doop -a context-insensitive -i com.example.some.jar --platform java_7
    $ ./doop -a context-insensitive -i some-app.apk --platform android_25_fulljars

To use a custom Java platform, you can use option `--use-local-java-platform`, for example:

    $ ./doop -a context-insensitive -i com.example.some.jar --platform java_11 --use-local-java-platform /path/to/java/11/installation


#### Main class (--main)
The main class to use as the entry point. This class must declare a method with signature `public static void main(String [])`. If not specified, Doop will try to infer this information from the manifest file of the provided jar file(s).

Example:

    $ ./doop -a context-insensitive -i com.example.some.jar --main com.example.some.Main

#### Timeout (-t, --timeout)
Specify the analysis execution timeout in minutes.

Example:

    $ ./doop -a context-insensitive -i com.example.some.jar -t 120

The above analysis will run for a maximum of 2 hours (120 minutes).

#### Analysis id (-id, --identifier)
The identifier of the analysis.

If the identifier is not specified, Doop will generate one automatically. Use this option if you prefer
to provide a human-friendly identifier to your analysis.

Example:

    $ ./doop -id myAnalysis

#### Packages (--regex)
The Java packages to treat as application code (not library code), to be exhaustively analyzed.

Example:

    $ ./doop --regex com.example.package1.*:com.example.package2.*

#### Native code scanner (--scan-native-code)
This option makes Doop scan the native dynamic libraries bundled in
.jar or .apk inputs, to find possible calls from JNI code to Java
code. For setup instructions, see the [project repository](https://github.com/plast-lab/native-scanner).

### Soufflé multithreading

Soufflé supports multithreading, so you can select the number of threads the analysis will run on by providing the --souffle-jobs argument to doop. For example:

    $ ./doop -i ../doop-benchmarks/dacapo-2006/antlr.jar -a context-insensitive --id antlr-ci --dacapo --souffle-jobs 12
