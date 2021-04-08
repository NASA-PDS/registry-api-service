package gov.nasa.pds.api.engineering.elasticsearch;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;

import gov.nasa.pds.api.engineering.lexer.SearchLexer;
import gov.nasa.pds.api.engineering.lexer.SearchParser;

public class Antlr4SearchListenerTest
{
	public static void main(String[] args)
	{
		Antlr4SearchListenerTest self = new Antlr4SearchListenerTest();
		boolean summary=true;
		
		
		summary &= self.verify_01();
		summary &= self.verify_02();
		System.out.println("test as a " + (summary ? "success" : "FAILURE"));
	}

	private BoolQueryBuilder run (String query)
	{
		CodePointCharStream input = CharStreams.fromString(query);
        SearchLexer lex = new SearchLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        SearchParser par = new SearchParser(tokens);
        ParseTree tree = par.query();
        // Walk it and attach our listener
        ParseTreeWalker walker = new ParseTreeWalker();
        Antlr4SearchListener listener = new Antlr4SearchListener();
        walker.walk(listener, tree);
     
		//System.out.println ("query string: " + query);
        //System.out.println("query tree: " + tree.toStringTree(par));
        //System.out.println("boolean query: " + listener.getBoolQuery().toString());
        return listener.getBoolQuery();
	}

	private boolean verify_01()
	{
		boolean result = true;
		BoolQueryBuilder query = this.run("lid eq *pdart14_meap*");
		
		result &= query.must().size() == 1;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof WildcardQueryBuilder;
		result &= ((WildcardQueryBuilder)query.must().get(0)).fieldName().equals("lid");
		result &= ((WildcardQueryBuilder)query.must().get(0)).value().equals("*pdart14_meap*");
		return result;
	}

	private boolean verify_02()
	{
		boolean result = true;
		BoolQueryBuilder query = this.run("( lid eq *pdart14_meap* )");
		
		result &= query.must().size() == 1;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof BoolQueryBuilder;
		query = (BoolQueryBuilder)query.must().get(0);
		result &= query.must().size() == 1;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof WildcardQueryBuilder;
		result &= ((WildcardQueryBuilder)query.must().get(0)).fieldName().equals("lid");
		result &= ((WildcardQueryBuilder)query.must().get(0)).value().equals("*pdart14_meap*");
		return result;
	}

	private boolean verify_99()
	{
		boolean result = true;
		return result;
	}
}
