# Critic

Finds the critical path in your Maven multi-module project. It does so based on
the dependency graph and on a [Maven
reactor](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
summary of build timings.

## Why

I started this project for my work at [DHIS2](https://dhis2.org/about/). I
analyzed the build of our Java application built using
[Maven](https://maven.apache.org/). It is a multi-module Maven project with 30+
Maven modules in a single [repository](https://github.com/dhis2/dhis2-core/). I
was especially interested in the duration of our tests. They seemed to
sometimes take long (which is relative :joy:) ~ 23min. I tried to figure out
where time is spent, and what we can do about it.

## Usage

Create a dependency graph of your Maven project using
https://github.com/ferstl/depgraph-maven-plugin in DOT file format.

```sh
mvn com.github.ferstl:depgraph-maven-plugin:aggregate \
    --file ~/your_multimodule_project/pom.xml \
    --batch-mode --no-transfer-progress \
    -DcreateImage=true -DimageFormat=png -DmergeScopes=true \
    -DoutputDirectory=~/somewhere \
    -Dincludes=org.hisp.dhis \
    -Dexcludes=org.hisp.dhis:dhis-web-embedded-jetty
```

*Note: its important to use mergeScopes so your each of your modules shows up
as one node in the graph. Otherwise you will potentially have multiple ones for
test, compile, ... Each of your modules only appears once in the reactor build
summary. I therefore only want to assign it one cost/weight.*

Compile and run

```sh
mvn clean package
java -cp target/critic-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.teleivo.critic.App \
  --dependency-graph ~/somewhere/dependency-graph.dot \
  --artifact-mapping ~/somewhere/maven_name_to_coordinates.csv \
  --build-log ~/somewhere/maven_build_log
  --output ~/somewhere/critical_path.dot
```

If all goes well you should now have a DOT file with your dependency graph and
the critical path highlighted.

It should also print something like

`Maven build order - critical path ends at org.hisp.dhis:dhis-web-api-test[PT6.949S] and takes 17.63min`

## What next?

Once you have identified your critical path you can either

* investigate how to reduce the time spent building & testing each module
* or extract the build & testing of a module and run that outside of a full
  `mvn clean install` that builds your entire multi-module project. This way
  you do not need to adhere to the Maven reactor build order and can run the
  tests straight away, concurrently to other modules tests. You will of course
  need to make sure that its dependencies are satisfied. So you might need to
  package them beforehand without running their tests. This process will likely
  adhere to the Maven reactor build order (if you run it using `mvn clean
  install -DskipTests -Dmaven.test.skip=true`).
* there might be other avenues I haven't tought about. If so please let me know
  :smile:

## Example Case Study

I started this project for my work at [DHIS2](https://dhis2.org/about/). I
analyzed the build of our Java application built using
[Maven](https://maven.apache.org/). It is a multi-module Maven project with 30+
Maven modules in a single [repository](https://github.com/dhis2/dhis2-core/).

`./example/` contains all files I used to for example analyze the test run of

https://github.com/dhis2/dhis2-core/pull/9145

You can recreate them by running

```sh
mvn clean package
java -cp target/critic-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.teleivo.critic.App \
  --dependency-graph example/PR_9145_dependency_graph.dot \
  --artifact-mapping example/maven_name_to_coordinates.csv \
  --build-log example/PR_9145_job_integration_test_step_run_integration_tests \
  --output example/PR_9145_critical_path.dot
```

[PR_9145_critical_path.png](./example/PR_9145_critical_path.png) shows the
critical path. It also highlighted that it takes > 7min to build and test
the module `dhis-service-analytics` which takes up a big chunk of the overall
duration. At the time we were also running our
integration tests without [Maven's parallel build feature](https://cwiki.apache.org/confluence/display/MAVEN/Parallel+builds+in+Maven+3).
So runs could take ~ 23min. We decided to run tests for `dhis-service-analytics`
in another job, so concurrently. Integration tests now run between 13min -
15min :tada:

## Limitations

The project was good enough for me as it is. That being said

* while the code to find the critical path is tested, it might still contain
  bugs. If so please let me know! :smile:
* the code for the CLI, creating the graph is not tested other than through
  manual tests.
* the Maven reactor build does not provide build durations in a form other than
  its log output like `[INFO] DHIS Reporting Service
  ............................. SUCCESS [ 46.054 s]`. The project was tested on
  build logs generated by Maven 3.x It might not work for future versions of
  Maven in case the duration format in the logs changes.

## Improvement Ideas

I doubt I will implement them, as the project does the job for me but maybe you
want to

* release the CLI as an executable binary using GraalVM
* pass in the repository and the maven build log as the only sources of
  information to generate the critical path graph. critic could generate the
  initial dependency graph itself, removing the manual step of creating the
  dependency graph. It could also parse the maven artifact names from the
  projects pom.xml files instead of needing a CSV with artifact coordinate to
  name mapping.

## Related

* https://github.com/teleivo/dhis2-github-action-metrics
* https://github.com/teleivo/github-action-metrics
