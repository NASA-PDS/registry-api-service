package gov.nasa.pds.api.engineering.elasticsearch;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import gov.nasa.pds.api.engineering.lexer.SearchLexer;
import gov.nasa.pds.api.engineering.lexer.SearchParser;

public class Antlr4SearchListenerTest
{
	public static void main(String[] args)
	{
		Antlr4SearchListenerTest self = new Antlr4SearchListenerTest();
		boolean result,summary=true;
		
		String expectation = "", query = "lid eq *pdart14_meap*";
		result = self.verify (expectation, query);
		summary = result & summary;
		System.out.println((result ? "success" : "FAILURE") + " - " + query);
		System.out.println("test as a " + (summary ? "success" : "FAILURE"));
	}

	private boolean verify (String expectation, String queryString)
	{
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();		
		CodePointCharStream input = CharStreams.fromString(queryString);
        SearchLexer lex = new SearchLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        SearchParser par = new SearchParser(tokens);
        ParseTree tree = par.query();
        // Walk it and attach our listener
        ParseTreeWalker walker = new ParseTreeWalker();
        Antlr4SearchListener listener = new Antlr4SearchListener(boolQuery);
        walker.walk(listener, tree);
     
		System.out.println ("query string: " + queryString);
        System.out.println("query tree: " + tree.toStringTree(par));
        System.out.println("boolean query: " + boolQuery.toString());
        return false;
	}
}
