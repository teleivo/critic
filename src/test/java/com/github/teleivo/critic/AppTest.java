package com.github.teleivo.critic;

import static com.github.teleivo.critic.App.criticalPath;
import static com.github.teleivo.critic.App.parseMavenReactorSummaryEntry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.junit.jupiter.api.Test;

class AppTest
{

    @Test
    void criticalPathGivenAnEmptyGraph()
    {

        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();
        List<DefaultWeightedEdge> cp = criticalPath( g );

        assertIterableEquals( Collections.emptyList(), cp );
    }

    @Test
    void criticalPathGivenOneVertex()
    {

        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();
        g.addVertex( 0 );
        List<DefaultWeightedEdge> cp = criticalPath( g );

        assertIterableEquals( Collections.emptyList(), cp );
    }

    @Test
    void criticalPathGivenOneEdge()
    {
        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();
        g.addVertex( 0 );
        g.addVertex( 1 );
        DefaultWeightedEdge e1 = g.addEdge( 0, 1 );

        List<DefaultWeightedEdge> cp = criticalPath( g );

        assertIterableEquals( List.of( e1 ), cp );
    }

    @Test
    void criticalPathGivenTwoConnectedEdges()
    {
        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();
        g.addVertex( 0 );
        g.addVertex( 1 );
        g.addVertex( 2 );
        DefaultWeightedEdge e1 = g.addEdge( 0, 1 );
        DefaultWeightedEdge e2 = g.addEdge( 1, 2 );

        List<DefaultWeightedEdge> cp = criticalPath( g );

        assertIterableEquals( List.of( e2, e1 ), cp );
    }

    @Test
    void criticalPathGivenTwoParallelEdges()
    {
        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();
        g.addVertex( 0 );
        g.addVertex( 1 );
        g.addVertex( 2 );
        g.addVertex( 3 );
        DefaultWeightedEdge e1 = g.addEdge( 0, 1 );
        DefaultWeightedEdge e2 = g.addEdge( 2, 3 );
        g.setEdgeWeight( e1, 1.0 );
        g.setEdgeWeight( e2, 3.0 );

        List<DefaultWeightedEdge> cp = criticalPath( g );

        assertIterableEquals( List.of( e2 ), cp );
    }

    @Test
    void criticalPathGivenAnEdgeFork()
    {
        Graph<Integer, DefaultWeightedEdge> g = GraphTypeBuilder
            .directed()
            .allowingMultipleEdges( false )
            .allowingSelfLoops( false )
            .weighted( true )
            .edgeClass( DefaultWeightedEdge.class )
            .vertexSupplier( SupplierUtil.createIntegerSupplier() )
            .buildGraph();
        g.addVertex( 0 );
        g.addVertex( 1 );
        g.addVertex( 2 );
        g.addVertex( 3 );
        DefaultWeightedEdge e1 = g.addEdge( 0, 1 );
        DefaultWeightedEdge e2 = g.addEdge( 1, 2 );
        DefaultWeightedEdge e3 = g.addEdge( 1, 3 );
        g.setEdgeWeight( e1, 1.0 );
        g.setEdgeWeight( e2, 1.0 );
        g.setEdgeWeight( e3, 2.0 );

        List<DefaultWeightedEdge> cp = criticalPath( g );

        assertIterableEquals( List.of( e3, e1 ), cp );
    }

    @Test
    void parseMavenReactorSummaryEntrySuccess()
    {
        assertArrayEquals( new String[] { "DHIS Node service", "4.543 s" },
            parseMavenReactorSummaryEntry(
                "2021-12-01T08:30:34.9304126Z [INFO] DHIS Node service .................................. SUCCESS [  4.543 s]" ) );
        assertArrayEquals( new String[] { "DHIS Core API Implementations", "03:00 min" },
            parseMavenReactorSummaryEntry(
                "2021-12-01T08:30:34.9308634Z [INFO] DHIS Core API Implementations ...................... SUCCESS [03:00 min]" ) );
        assertArrayEquals( new String[] { "DHIS ACL service", "0.980 s" },
            parseMavenReactorSummaryEntry(
                "[INFO] DHIS ACL service ................................... FAILURE [  0.980 s]" ) );
    }

    @Test
    void parseMavenReactorSummaryEntryGivenMissingDuration()
    {
        assertNull( parseMavenReactorSummaryEntry(
            "2021-12-01T08:30:34.9304126Z [INFO] DHIS Node service .................................. SUCCESS" ) );
        assertNull( parseMavenReactorSummaryEntry(
            "[INFO] DHIS Support Commons ............................... SKIPPED" ) );
    }
}
