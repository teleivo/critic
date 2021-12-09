package com.github.teleivo.critic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.github.teleivo.critic.maven.Module;
import com.github.teleivo.critic.maven.ReactorSummary;

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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command( name = "critic", description = "Highlights the critical path in a Maven dependency graph based on a Maven reactor dependency graph and summary." )
public class App implements Callable<Integer>
{

    private static final String GRAPH_LABEL_WEIGHT = "weight";

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
        Map<Integer, Module> modules = new HashMap<>();
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
            if ( p.getSecond().equals( GRAPH_LABEL_WEIGHT ) )
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

        EdgeReversedGraph<Integer, DefaultWeightedEdge> rg = new EdgeReversedGraph<>( g );
        List<DefaultWeightedEdge> criticalEdges = criticalPath( rg );
        if ( criticalEdges.isEmpty() )
        {
            System.out.println( "Maven build order - no critical path found" );
            return 0;
        }
        EdgeWeightSummary summary = EdgeWeightSummary.of( rg, criticalEdges );

        String label = String.format( "Maven build order - critical path ends at %s and takes %.2fmin",
            modules.get( summary.getMaxTarget() ), summary.getTotal() / 60 );
        System.out.println( label );

        DOTExporter<Integer, DefaultWeightedEdge> exporter = new DOTExporter<>();
        exporter.setGraphIdProvider( () -> "\"maven build order\"" );
        exporter.setGraphAttributeProvider( () -> {
            Map<String, Attribute> attrs = new HashMap<>();
            attrs.put( "label", DefaultAttribute.createAttribute( String.format( "\"%s\"", label ) ) );
            return attrs;
        } );
        exporter.setVertexIdProvider( v -> "\"" + modules.get( v ) + "\"" );
        exporter.setVertexIdProvider( v -> {
            return "\"" + modules.get( v ).getCoordinates() + "\"";
        } );
        exporter.setVertexAttributeProvider( v -> {
            Map<String, Attribute> attrs = new LinkedHashMap<>();
            attrs.put( "label", DefaultAttribute.createAttribute( modules.get( v ).getArtifactId() ) );
            attrs.put( "tooltip", DefaultAttribute.createAttribute( modules.get( v ).toString() ) );
            attrs.put( "fontsize", DefaultAttribute.createAttribute( 16 ) );
            attrs.put( "shape", DefaultAttribute.createAttribute( "box" ) );
            attrs.put( "style", DefaultAttribute.createAttribute( "rounded" ) );
            return attrs;
        } );
        exporter.setEdgeAttributeProvider( e -> {
            Map<String, Attribute> attrs = new HashMap<>();
            double weight = rg.getEdgeWeight( e );
            attrs.put( GRAPH_LABEL_WEIGHT, DefaultAttribute.createAttribute( weight ) );
            attrs.put( "label", DefaultAttribute.createAttribute( String.format( "%.2fmin", weight / 60 ) ) );
            attrs.put( "fontsize", DefaultAttribute.createAttribute( 15 ) );
            if ( criticalEdges.contains( e ) )
            {
                attrs.put( "penwidth",
                    DefaultAttribute.createAttribute( penWidth( summary.min, summary.max, weight ) ) );
                attrs.put( "color", DefaultAttribute.createAttribute( "#b22800" ) );
            }
            return attrs;
        } );
        exporter.exportGraph( rg, output );
        return 0;
    }

    static class EdgeWeightSummary
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

        List<DefaultWeightedEdge> criticalEdges = new ArrayList<>();
        for ( Integer s = maxSources.get( maxTarget ),
            t = maxTarget; s != null; t = s, s = maxSources.get( t ) )
        {
            criticalEdges.add( g.getEdge( s, t ) );
        }
        return criticalEdges;
    }

    /**
     * Adjust an edge weight relative to its contribution
     *
     **/
    static double penWidth( double min, double max, double weight )
    {
        final double minPenWidth = 1;
        final double maxPenWidth = 10;
        return minPenWidth + (((maxPenWidth - minPenWidth) * (weight - min)) / (max - min));

    }

    public static void main( String[] args )
    {
        int exitCode = new CommandLine( new App() ).execute( args );
        System.exit( exitCode );
    }
}
