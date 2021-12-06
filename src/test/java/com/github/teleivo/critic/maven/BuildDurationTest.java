package com.github.teleivo.critic.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BuildDurationTest
{
    @Test
    void parseDurationSuccess()
    {
        assertEquals( "PT03M07S", BuildDuration.parse( "03:07 min" ) );
        assertEquals( "PT12M27S", BuildDuration.parse( "12:27 min" ) );
        assertEquals( "PT0.980S", BuildDuration.parse( "0.980 s" ) );
        assertEquals( "PT46.054S", BuildDuration.parse( "46.054 s" ) );
    }

    @Test
    void parseDurationFailsGivenMissingDecimalPointInSecondsFormat()
    {
        assertEquals( "", BuildDuration.parse( "46 s" ) );
    }

    @Test
    void parseDurationFailsGivenMissingSecondsInMinuteFormat()
    {
        assertEquals( "", BuildDuration.parse( "03 min" ) );
    }

    @Test
    void parseDurationGivenHoursIsNotImplemented()
    {
        assertEquals( "", BuildDuration.parse( "03:34 h" ) );
    }

    @Test
    void parseDurationGivenEmptyString()
    {
        assertEquals( "", BuildDuration.parse( "" ) );
    }
}
