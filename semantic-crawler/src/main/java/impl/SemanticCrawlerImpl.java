package impl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;

import com.crawler.SemanticCrawler;

public class SemanticCrawlerImpl implements SemanticCrawler {

    private Set<String> visitados = new HashSet<>();
    
    @Override
    public void search(org.apache.jena.rdf.model.Model graph, String resourceURI) {
        CharsetEncoder enc = Charset.forName("ISO-8859-1").newEncoder();
        if (!enc.canEncode(resourceURI)) {
            
            return;
        }

        if (visitados.contains(resourceURI)) {
            return;
        }
        visitados.add(resourceURI);

        Model docAtual = ModelFactory.createDefaultModel();
        try {
            String url = resourceURI;
            if (resourceURI.contains("dbpedia.org/resource/")) {
                url = resourceURI.replace("dbpedia.org/resource/", "dbpedia.org/data/") + ".rdf";
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "application/rdf+xml");

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == 303) {
                String newUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/rdf+xml");
            }

            RDFDataMgr.read(docAtual, conn.getInputStream(), Lang.RDFXML);
        } catch (Exception e) {
            System.out.println("Erro ao dereferenciar: " + resourceURI + " → " + e.getMessage());
            return;
        }

        Resource recurso = docAtual.getResource(resourceURI);
        StmtIterator stmts = docAtual.listStatements(recurso, null, (RDFNode) null);

        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();
            graph.add(stmt);
            
            // se o objeto for blank node, tratar depois
            if (stmt.getObject().isAnon()) {
                coletarBlankNode(docAtual, stmt.getObject(), graph);
            }
        }

        Property sameAs = OWL.sameAs;

        // uri eh o sujeito --> segue o objeto
        StmtIterator sameAsObj = docAtual.listStatements(recurso, sameAs, (RDFNode) null);
        while (sameAsObj.hasNext()) {
            Statement stmt = sameAsObj.nextStatement();
            RDFNode objeto = stmt.getObject();
            if (objeto.isURIResource()) {
                search(graph, objeto.asResource().getURI());
            } else if (objeto.isAnon()) {
                coletarBlankNode(docAtual, objeto, graph);
            }
        }

        // uri eh o objeto --> segue o sujeito
        StmtIterator sameAsSuj = docAtual.listStatements(null, sameAs, recurso);
        while (sameAsSuj.hasNext()) {
            Statement stmt = sameAsSuj.nextStatement();
            Resource sujeito = stmt.getSubject();
            if (sujeito.isURIResource()) {
                search(graph, sujeito.getURI());
            } else if (sujeito.isAnon()) {
                coletarBlankNode(docAtual, sujeito, graph);
            }
        }
    }

    private void coletarBlankNode(Model docAtual, RDFNode no, Model graph) {
        StmtIterator stmts = docAtual.listStatements(no.asResource(), null, (RDFNode) null);
        
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();
            graph.add(stmt);
            
            // se o objeto também for blank node, repete recursivamente
            if (stmt.getObject().isAnon()) {
                coletarBlankNode(docAtual, stmt.getObject(), graph);
            }
        }
    }
    
}