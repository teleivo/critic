# Critic

Finds the critical path in your Maven multi-module project. It does so based on
the dependency graph and on a [Maven
reactor](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
summaris build timings.

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
    --file ~/code/dhis2/core/dhis-2/pom.xml \
    --batch-mode --no-transfer-progress \
    -DcreateImage=true -DimageFormat=png -DmergeScopes=true \
    -DoutputDirectory=../../deps \
    -Dincludes=org.hisp.dhis:dhis-service-dxf2,org.hisp.dhis:dhis-service-core,org.hisp.dhis:dhis-service-tracker,org.hisp.dhis:dhis-service-reporting,org.hisp.dhis:dhis-service-validation,org.hisp.dhis:dhis-support-audit,org.hisp.dhis:dhis-service-program-rule,org.hisp.dhis:dhis-service-analytics,org.hisp.dhis:dhis-service-administration
```

Compile and run

```sh
mvn clean package
java -cp target/critic-1.0-SNAPSHOT-jar-with-dependencies.jar \
  com.github.teleivo.critic.App -d src/main/resources/dependency-graph.dot -o foo.dot
```

If all goes well you should now have a DOT file with your dependency graph and
the critical path highlighted.

It should also print something like

`Maven build order - critical path ends at org.hisp.dhis:dhis-service-reporting:jar and takes 12.30min`

## What next?

Once you have identified your critical path you can either

* investigate how to reduce the time spend building & testing each module
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

## Limitations

The project was good enough for me as it is. That being said

* while the code to find the critical path is tested, it might still contain
  bugs. If so please let me know!
* the code for the CLI, creating the graph is not tested other than through
  manual tests.
* you need to build it yourself

## Related

* https://github.com/teleivo/dhis2-github-action-metrics
* https://github.com/teleivo/github-action-metrics
