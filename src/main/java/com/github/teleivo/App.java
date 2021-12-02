package com.github.teleivo;

import java.util.HashMap;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.nio.dot.DOTImporter;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.jgrapht.util.SupplierUtil;

public class App {
    public static void main(String[] args) {

        Graph<String, DefaultWeightedEdge> g = new DefaultDirectedWeightedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.createDefaultWeightedEdgeSupplier());

        Map<String, String> module = new HashMap<>();
        DOTImporter<String, DefaultWeightedEdge> importer = new DOTImporter<>();
        importer.addVertexAttributeConsumer((p, a) -> {
            if (p.getSecond() == "ID") {
                module.put(p.getFirst(), a.getValue());
            }
        });
        importer.importGraph(g, App.class.getClassLoader().getResourceAsStream("dependency-graph.dot"));

        // TODO add root node and weights into graph like before
        EdgeReversedGraph<String, DefaultWeightedEdge> rg = new EdgeReversedGraph<>(g);
        TopologicalOrderIterator<String, DefaultWeightedEdge> iterator = new TopologicalOrderIterator<>(rg);
        while (iterator.hasNext()) {
            String v = iterator.next();
            System.out.println(module.get(v));
        }

        // TODO visualize critical path. Ideally, create a png and dot file
    }
}
