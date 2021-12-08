package com.github.teleivo.critic.maven;

import java.time.Duration;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Module
{

    private String groupId;

    private String artifactId;

    private Duration buildDuration;

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public Duration getBuildDuration()
    {
        return buildDuration;
    }

    public String getCoordinates()
    {
        return groupId + ":" + artifactId;
    }

    // TODO cleanup constructor duplication
    public Module( final String coordinates )
    {
        String[] components = coordinates.split( ":" );
        if ( components.length < 2 )
        {
            throw new IllegalArgumentException(
                String.format( "coordinates need at least 2 components '%s' has less", coordinates ) );
        }
        this.groupId = components[0];
        this.artifactId = components[1];
    }

    public Module( final String coordinates, final Duration buildDuration )
    {
        String[] components = coordinates.split( ":" );
        if ( components.length < 2 )
        {
            throw new IllegalArgumentException(
                String.format( "coordinates need at least 2 components '%s' has less", coordinates ) );
        }
        this.groupId = components[0];
        this.artifactId = components[1];
        this.buildDuration = buildDuration;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( getCoordinates() );
        if ( buildDuration != null )
        {
            sb.append( "[" )
                .append( buildDuration )
                .append( "]" );
        }
        return sb.toString();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;

        if ( o == null || getClass() != o.getClass() )
            return false;

        Module that = (Module) o;

        return new EqualsBuilder()
            .append( groupId, that.groupId )
            .append( artifactId, that.artifactId )
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder( 17, 37 )
            .append( groupId )
            .append( artifactId )
            .toHashCode();
    }
}
