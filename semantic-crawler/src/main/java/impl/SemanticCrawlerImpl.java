package impl;

import java.util.HashSet;
import java.util.Set;

import com.crawler.SemanticCrawler;

public class SemanticCrawlerImpl implements SemanticCrawler {

    private Set<String> visitados = new HashSet<>();
    
    @Override
    public void search(org.apache.jena.rdf.model.Model graph, String resourceURI) {
        // método sem implementação
        
    }
    
}
