package com.github.teleivo.critic.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildDuration
{
    /**
     * Parses durations shown in minutes and seconds format in a Maven reactor
     * summary into a representation parseable by
     * {@link java.time.Duration#parse}
     *
     */
    public static String parse( String in )
    {
        // Here is the code producing the durations in the Maven reactor build
        // summary
        // https://github.com/apache/maven/blob/a20230829c1624cfea89caef87d7b213f51971d6/maven-embedder/src/main/java/org/apache/maven/cli/CLIReportingUtils.java#L161-L194
        if ( in.contains( "min" ) )
        {

            Pattern DURATION_MINUTES = Pattern.compile( "(\\d+):(\\d+)" );
            Matcher m = DURATION_MINUTES.matcher( in );
            if ( !m.find() )
            {
                return "";
            }

            return "PT" + m.group( 1 ) + "M" + m.group( 2 ) + "S";
        }
        else if ( in.contains( "s" ) )
        {
            Pattern DURATION_SECONDS = Pattern.compile( "(\\d+.\\d+)" );
            Matcher m = DURATION_SECONDS.matcher( in );
            if ( !m.find() )
            {
                return "";
            }

            return "PT" + m.group( 1 ) + "S";
        }
        return "";
    }
}
