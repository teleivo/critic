package com.github.teleivo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.dot.DOTImporter;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.jgrapht.util.SupplierUtil;

public class App
{

    static class MavenModule
    {
        String groupId;

        String artifactId;

        String type;

        public static MavenModule of( final String coordinates )
        {
            String[] components = coordinates.split( ":" );
            MavenModule m = new MavenModule();
            if ( components.length > 2 )
            {
                m.groupId = components[0];
                m.artifactId = components[1];
                m.type = components[2];
            }
            else if ( components.length > 1 )
            {
                m.groupId = components[0];
                m.artifactId = components[1];
            }
            else
            {
                m.groupId = components[0];
            }
            return m;
        }

        @Override
        public String toString()
        {
            return groupId + ":" + artifactId + ":" + type;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
                return true;

            if ( o == null || getClass() != o.getClass() )
                return false;

            MavenModule that = (MavenModule) o;

            return new EqualsBuilder().append( groupId, that.groupId ).append( artifactId, that.artifactId )
                .append( type, that.type ).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder( 17, 37 ).append( groupId ).append( artifactId ).append( type ).toHashCode();
        }
    }

    static List<DefaultWeightedEdge> criticalPath( Graph<Integer, DefaultWeightedEdge> g )
    {
        int maxTarget = 0;
        double maxCost = 0.0;
        Map<Integer, Double> maxCosts = new HashMap<>();
        Map<Integer, Integer> maxSources = new HashMap<>();

        TopologicalOrderIterator<Integer, DefaultWeightedEdge> it = new TopologicalOrderIterator<>( g );
        while ( it.hasNext() )
        {
            Integer v = it.next();
            double max = 0;
            Integer maxSource = null;
            for ( DefaultWeightedEdge e : g.incomingEdgesOf( v ) )
            {
                Integer s = g.getEdgeSource( e );
                double cost = maxCosts.getOrDefault( s, 0.0 ) + g.getEdgeWeight( e );
                if ( cost > max )
                {
                    max = cost;
                    maxSource = s;
                }
            }
            maxCosts.put( v, max );
            maxSources.put( v, maxSource );
            if ( max > maxCost )
            {
                maxCost = max;
                maxTarget = v;
            }
        }

        List<DefaultWeightedEdge> criticalEdges = new ArrayList<>();
        for ( Integer s = maxSources.get( maxTarget ),
            t = maxTarget; s != null; t = s, s = maxSources.get( t ) )
        {
            criticalEdges.add( g.getEdge( s, t ) );
        }
        return criticalEdges;
    }

    public static void main( String[] args )
    {

        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();
        Map<Integer, MavenModule> modules = new HashMap<>();
        DOTImporter<Integer, DefaultWeightedEdge> importer = new DOTImporter<>();
        importer.addVertexAttributeConsumer( ( p, a ) -> {
            if ( p.getSecond() == "ID" )
            {
                modules.put( p.getFirst(), MavenModule.of( a.getValue() ) );
            }
        } );

        // Note: the DOTImporter ignores weights unlike JSONImporter and others
        // :( So I have to set them explicitly
        importer.addEdgeAttributeConsumer( ( p, attr ) -> {
            if ( p.getSecond().equals( "weight" ) )
            {
                g.setEdgeWeight( p.getFirst(), Double.parseDouble( attr.getValue() ) );
            }
        } );
        importer.importGraph( g, App.class.getClassLoader().getResourceAsStream( "dependency-graph.dot" ) );

        // Note: add root node and connect independent modules to it. This is
        // necessary so that the time it takes to
        // build such a module shows up in the graph. Think of the root node as
        // the maven command starting the
        // build.
        // TODO this edge needs the proper weight
        Integer root = g.addVertex();
        for ( Integer v : g.vertexSet() )
        {
            if ( v != root && g.outDegreeOf( v ) == 0 )
            {
                g.addEdge( v, root );
            }
        }
        modules.put( root, MavenModule.of( "root:root" ) );

        EdgeReversedGraph<Integer, DefaultWeightedEdge> rg = new EdgeReversedGraph<>( g );
        List<DefaultWeightedEdge> criticalEdges = criticalPath( rg );
        double totalCost = 0.0;
        Integer maxTarget = null;
        for ( DefaultWeightedEdge e : criticalEdges )
        {
            if ( maxTarget == null )
            {
                maxTarget = rg.getEdgeTarget( e );
            }
            totalCost += rg.getEdgeWeight( e );
        }

        String label = String.format( "Maven build order - critical path ends at %s and takes %.2fmin",
            modules.get( maxTarget ), totalCost / 60 );
        System.out.println( label );

        // TODO can I directly create a png alongside the dot file?
        DOTExporter<Integer, DefaultWeightedEdge> exporter = new DOTExporter<>();
        exporter.setGraphIdProvider( () -> "\"maven build order\"" );
        exporter.setGraphAttributeProvider( () -> {
            Map<String, Attribute> attrs = new HashMap<>();
            attrs.put( "label", DefaultAttribute.createAttribute( String.format( "\"%s\"", label ) ) );
            return attrs;
        } );
        exporter.setVertexIdProvider( v -> "\"" + modules.get( v ) + "\"" );
        exporter.setVertexAttributeProvider( v -> {
            Map<String, Attribute> attrs = new LinkedHashMap<>();
            attrs.put( "label", DefaultAttribute.createAttribute( modules.get( v ).artifactId ) );
            attrs.put( "fontsize", DefaultAttribute.createAttribute( 16 ) );
            attrs.put( "shape", DefaultAttribute.createAttribute( "box" ) );
            attrs.put( "style", DefaultAttribute.createAttribute( "rounded" ) );
            return attrs;
        } );
        exporter.setEdgeAttributeProvider( e -> {
            Map<String, Attribute> attrs = new HashMap<>();
            double weight = rg.getEdgeWeight( e );
            attrs.put( "weight", DefaultAttribute.createAttribute( weight ) );
            attrs.put( "label", DefaultAttribute.createAttribute( String.format( "%.2fmin", weight / 60 ) ) );
            attrs.put( "fontsize", DefaultAttribute.createAttribute( 15 ) );
            if ( criticalEdges.contains( e ) )
            {
                attrs.put( "penwidth", DefaultAttribute.createAttribute( 2 ) );
                attrs.put( "color", DefaultAttribute.createAttribute( "#b22800" ) );
            }
            return attrs;
        } );
        exporter.exportGraph( rg, new File( "critical_path.dot" ) );
    }
}
