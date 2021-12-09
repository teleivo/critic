package com.github.teleivo.critic;

import java.io.File;
import java.util.concurrent.Callable;

import com.github.teleivo.critic.maven.CriticalPath;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command( name = "critic", description = "Highlights the critical path in a Maven dependency graph based on a Maven reactor dependency graph and summary." )
public class App implements Callable<Integer>
{

    @Option( names = { "-d",
        "--dependency-graph" }, required = true, description = "Input DOT file of Maven dependency graph generated using https://github.com/ferstl/depgraph-maven-plugin" )
    private File dependencyGraph;

    @Option( names = { "-b",
        "--build-log" }, required = true, description = "Maven build log containing the Maven 'Reactor Summary for' build timings.\nYou can run a build with '--log-file' to directly store it in a file." )
    private File mavenBuildLog;

    @Option( names = { "-a",
        "--artifact-mapping" }, required = true, description = "CSV mapping Maven project names to project coordinates.\nExpects 2 columns [name,coordinate]" )
    private File mavenArtifactMapping;

    @Option( names = { "-o",
        "--output" }, required = true, description = "Destination where DOT file with highlighted critical path will be written to" )
    private File output;

    @Option( names = "--help", usageHelp = true, description = "Display this help and exit" )
    private boolean help;

    @Override
    public Integer call()
        throws Exception
    {

        CriticalPath path = new CriticalPath( mavenArtifactMapping, mavenBuildLog, dependencyGraph );
        path.exportToDOT( output );
        return 0;
    }

    public static void main( String[] args )
    {
        int exitCode = new CommandLine( new App() ).execute( args );
        System.exit( exitCode );
    }
}
