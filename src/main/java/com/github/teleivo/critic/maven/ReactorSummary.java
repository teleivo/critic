package com.github.teleivo.critic.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReactorSummary
{
    private static final Pattern PROJECT_DURATION = Pattern.compile( "\\] (.+) \\.+[A-Za-z\\s\\[]+(.+)\\]" );

    public static Map<Module, Module> parse( Path artifactMapping, Path buildLog )
        throws IOException
    {
        final Map<String, String> mavenCoordinates = parseNameToCoordinates( artifactMapping );
        final Map<Module, Module> reactorModules = new HashMap<>();
        try ( Scanner sc = new Scanner( buildLog ) )
        {
            boolean start = false;
            boolean end = false;
            while ( sc.hasNextLine() )
            {
                String l = sc.nextLine();
                if ( !start && l.contains( "Reactor Summary" ) )
                {
                    start = true;
                }
                else if ( start && l.contains( "BUILD" ) )
                {
                    end = true;
                }
                else if ( start && !end )
                {
                    String[] entry = parseReactorSummaryEntry( l );
                    if ( entry == null )
                    {
                        continue;
                    }
                    String coordinates = mavenCoordinates.get( entry[0] );
                    if ( coordinates == null )
                    {
                        throw new IllegalArgumentException(
                            String.format( "Cannot find maven project coordinates for given name '%s'", entry[0] ) );
                    }
                    Duration d = Duration.parse( BuildDuration.parse( entry[1] ) );
                    Module m = new Module( coordinates, d );
                    reactorModules.put( m, m );
                }
            }
        }
        return reactorModules;
    }

    private static Map<String, String> parseNameToCoordinates( Path csv )
        throws IOException
    {
        // NOTE: it does not handle a CSV header differently than the rest of
        // the CSV

        try (
            Stream<String> lines = Files.lines( csv ); )
        {
            Map<String, String> resultMap = lines.map(
                line -> line.split( "," ) )
                .collect(
                    Collectors.toMap( line -> line[0].trim(), line -> line[1].trim() ) );
            return resultMap;
        }
    }

    static String[] parseReactorSummaryEntry( final String in )
    {
        Matcher m = PROJECT_DURATION.matcher( in );
        if ( !m.find() )
        {
            return null;
        }

        return new String[] { m.group( 1 ), m.group( 2 ) };
    }

}
