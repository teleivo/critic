package com.github.teleivo.critic.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

/**
 * CriticalPath
 */
public class CriticalPath
{

    private static final String GRAPH_ATTRIBUTE_WEIGHT = "weight";

    private static final String GRAPH_ATTRIBUTE_LABEL = "label";

    private static final String GRAPH_ATTRIBUTE_FONTSIZE = "fontsize";

    private static final String GRAPH_ATTRIBUTE_COLOR = "color";

    private static final String GRAPH_ATTRIBUTE_TOOLTIP = "tooltip";

    private static final String GRAPH_ATTRIBUTE_SHAPE = "shape";

    private static final String GRAPH_ATTRIBUTE_STYLE = "style";

    private static final String GRAPH_ATTRIBUTE_PENWIDTH = "penwidth";

    private static final double MIN_PENWIDTH = 1.0;

    private static final double MAX_PENWIDTH = 10.0;

    private EdgeReversedGraph<Integer, DefaultWeightedEdge> rg;

    private List<DefaultWeightedEdge> criticalPath;

    private Map<Integer, Module> modules;

    public CriticalPath( File mavenArtifactMapping, File mavenBuildLog, File dependencyGraph )
        throws IOException
    {
        // Note: using a Map instead of a Set because Set does not provide a
        // get(). I need to retrieve the module in the reactorModules "set"
        // since it contains the build durations
        // which I do not have in my "query" module that I get from the
        // maven dependency graph
        final Map<Module, Module> reactorModules = ReactorSummary.parse(
            mavenArtifactMapping.toPath(),
            mavenBuildLog.toPath() );

        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();

        modules = new HashMap<>();
        DOTImporter<Integer, DefaultWeightedEdge> importer = new DOTImporter<>();
        List<Module> missingDurations = new ArrayList<>();
        importer.addVertexAttributeConsumer( ( p, a ) -> {
            if ( p.getSecond() == "ID" )
            {
                Module m = new Module( a.getValue() );
                if ( !reactorModules.containsKey( m ) )
                {
                    missingDurations.add( m );
                    return;
                }
                modules.put( p.getFirst(), reactorModules.get( m ) );
            }
        } );

        // Note: the DOTImporter ignores weights unlike JSONImporter and others
        // :( So I have to set them explicitly
        importer.addEdgeAttributeConsumer( ( p, attr ) -> {
            if ( p.getSecond().equals( GRAPH_ATTRIBUTE_WEIGHT ) )
            {
                g.setEdgeWeight( p.getFirst(), Double.parseDouble( attr.getValue() ) );
            }
        } );
        importer.importGraph( g, dependencyGraph );

        if ( !missingDurations.isEmpty() )
        {
            throw new IllegalArgumentException(
                String.format(
                    "no build duration in reactor summary for modules found in dependency graph %s",
                    missingDurations ) );
        }

        Integer root = g.addVertex();
        modules.put( root, new Module( "root:root" ) );
        for ( Integer v : g.vertexSet() )
        {
            if ( v == root )
            {
                continue;
            }
            // Note: add root node and connect independent modules to it. This
            // is
            // necessary so that the time it takes to
            // build such a module shows up in the graph. Think of the root node
            // as
            // the maven command starting the
            // build.
            if ( g.outDegreeOf( v ) == 0 )
            {
                g.addEdge( v, root );
            }
            // Add edge weights from reactor build summary
            Module m = modules.get( v );
            for ( DefaultWeightedEdge e : g.outgoingEdgesOf( v ) )
            {
                g.setEdgeWeight( e, m.getBuildDuration().getSeconds() );
            }
        }

        this.rg = new EdgeReversedGraph<>( g );
        this.criticalPath = criticalPath( rg );
    }

    public void exportToDOT( File output )
    {

        if ( criticalPath.isEmpty() )
        {
            System.out.println( "Maven build order - no critical path found" );
            return;
        }
        EdgeWeightSummary summary = EdgeWeightSummary.of( rg, criticalPath );

        String label = String.format( "Maven build order - critical path ends at %s and takes %.2fmin",
            modules.get( summary.getMaxTarget() ), summary.getTotal() / 60 );
        System.out.println( label );

        DOTExporter<Integer, DefaultWeightedEdge> exporter = new DOTExporter<>();
        exporter.setGraphIdProvider( () -> "\"maven build order\"" );
        exporter.setGraphAttributeProvider( () -> {
            Map<String, Attribute> attrs = new HashMap<>();
            attrs.put( GRAPH_ATTRIBUTE_LABEL, DefaultAttribute.createAttribute( String.format( "\"%s\"", label ) ) );
            return attrs;
        } );
        exporter.setVertexIdProvider( v -> "\"" + modules.get( v ) + "\"" );
        exporter.setVertexIdProvider( v -> {
            return "\"" + modules.get( v ).getCoordinates() + "\"";
        } );
        exporter.setVertexAttributeProvider( v -> {
            Map<String, Attribute> attrs = new LinkedHashMap<>();
            attrs.put( GRAPH_ATTRIBUTE_LABEL, DefaultAttribute.createAttribute( modules.get( v ).getArtifactId() ) );
            attrs.put( GRAPH_ATTRIBUTE_TOOLTIP, DefaultAttribute.createAttribute( modules.get( v ).toString() ) );
            attrs.put( GRAPH_ATTRIBUTE_FONTSIZE, DefaultAttribute.createAttribute( 16 ) );
            attrs.put( GRAPH_ATTRIBUTE_SHAPE, DefaultAttribute.createAttribute( "box" ) );
            attrs.put( GRAPH_ATTRIBUTE_STYLE, DefaultAttribute.createAttribute( "rounded" ) );
            return attrs;
        } );
        exporter.setEdgeAttributeProvider( e -> {
            Map<String, Attribute> attrs = new HashMap<>();
            double weight = rg.getEdgeWeight( e );
            attrs.put( GRAPH_ATTRIBUTE_WEIGHT, DefaultAttribute.createAttribute( weight ) );
            attrs.put( GRAPH_ATTRIBUTE_LABEL,
                DefaultAttribute.createAttribute( String.format( "%.2fmin", weight / 60 ) ) );
            attrs.put( GRAPH_ATTRIBUTE_FONTSIZE, DefaultAttribute.createAttribute( 15 ) );
            if ( criticalPath.contains( e ) )
            {
                attrs.put( GRAPH_ATTRIBUTE_PENWIDTH,
                    DefaultAttribute.createAttribute( penWidth( summary.min, summary.max, weight ) ) );
                attrs.put( GRAPH_ATTRIBUTE_COLOR, DefaultAttribute.createAttribute( "#b22800" ) );
            }
            return attrs;
        } );
        exporter.exportGraph( rg, output );
    }

    private static class EdgeWeightSummary
    {

        double min;

        double max;

        double total;

        Integer maxTarget;

        static <K, V> EdgeWeightSummary of( Graph<Integer, DefaultWeightedEdge> g, List<DefaultWeightedEdge> edges )
        {
            double total = 0.0;
            double min = Integer.MAX_VALUE;
            double max = Integer.MIN_VALUE;
            Integer maxTarget = null;
            for ( DefaultWeightedEdge e : edges )
            {
                double weight = g.getEdgeWeight( e );
                if ( maxTarget == null )
                {
                    maxTarget = g.getEdgeTarget( e );
                }
                if ( weight > max )
                {
                    max = weight;
                }
                if ( weight < min )
                {
                    min = weight;
                }
                total += weight;
            }
            EdgeWeightSummary s = new EdgeWeightSummary();
            s.min = min;
            s.max = max;
            s.total = total;
            s.maxTarget = maxTarget;
            return s;
        }

        public double getMin()
        {
            return min;
        }

        public double getMax()
        {
            return max;
        }

        public double getTotal()
        {
            return total;
        }

        public Integer getMaxTarget()
        {
            return maxTarget;
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

        List<DefaultWeightedEdge> path = new ArrayList<>();
        for ( Integer s = maxSources.get( maxTarget ),
            t = maxTarget; s != null; t = s, s = maxSources.get( t ) )
        {
            path.add( g.getEdge( s, t ) );
        }
        return path;
    }

    /**
     * Adjust an edge weight relative to its contribution
     *
     **/
    static double penWidth( double min, double max, double weight )
    {
        return MIN_PENWIDTH + (((MAX_PENWIDTH - MIN_PENWIDTH) * (weight - min)) / (max - min));

    }
}
