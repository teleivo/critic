package com.github.teleivo.critic.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ModuleTest
{

    @Test
    void equalsAndHashCodeAllowMapUsage()
    {
        // Ensure that I can populate a map of modules with information from
        // the maven reactor summary and retrieve a module with information
        // I have in the dependency graph

        Map<Module, Module> map = new HashMap<>();

        Module m1 = new Module( "org.hisp.dhis:dhis-support-commons", Duration.ofSeconds( 2 ) );
        Module m2 = new Module( "org.hisp.dhis:dhis-support-commons" );

        map.put( m1, m1 );

        Module actual = map.get( m2 );

        assertEquals( m1.getGroupId(), actual.getGroupId() );
        assertEquals( m1.getArtifactId(), actual.getArtifactId() );
        assertEquals( m1.getBuildDuration(), actual.getBuildDuration() );
    }
}
