package com.crawler;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import impl.SemanticCrawlerImpl;

public class App {
    public static void main(String[] args) {
        Model graph = ModelFactory.createDefaultModel();
        SemanticCrawler crawler = new SemanticCrawlerImpl();
        
        crawler.search(graph, "http://dbpedia.org/resource/Zico");
        
        System.out.println("Triplas coletadas: " + graph.size());
        graph.write(System.out, "TURTLE");
    }
}