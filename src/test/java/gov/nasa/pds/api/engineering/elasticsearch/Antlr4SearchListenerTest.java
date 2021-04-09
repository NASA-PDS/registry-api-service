package gov.nasa.pds.api.engineering.elasticsearch;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;

import gov.nasa.pds.api.engineering.lexer.SearchLexer;
import gov.nasa.pds.api.engineering.lexer.SearchParser;

public class Antlr4SearchListenerTest
{
	public static void main(String[] args)
	{
		Antlr4SearchListenerTest self = new Antlr4SearchListenerTest();
		boolean summary=true;
		
		summary &= self.verify_01(); // wildcard eq
		summary &= self.verify_02(); // wildcard ne
		summary &= self.verify_03(); // not a wildcard with * and ? in it
		summary &= self.verify_04(); // grouping
		summary &= self.verify_05(); // grouping gt and lt
		summary &= self.verify_06(); // grouping ge and le
		System.out.println("test is a " + (summary ? "success" : "FAILURE"));
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
		String qs = "lid eq *pdart14_meap";
		BoolQueryBuilder query = this.run(qs);
		
		result &= query.must().size() == 1;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof WildcardQueryBuilder;
		result &= ((WildcardQueryBuilder)query.must().get(0)).fieldName().equals("lid");
		result &= ((WildcardQueryBuilder)query.must().get(0)).value().equals("*pdart14_meap");
		System.out.println((result ? "success" : "FAILURE") + " - "  + qs);
		return result;
	}

	private boolean verify_02()
	{
		boolean result = true;
		String qs = "lid ne pdart14_meap?";
		BoolQueryBuilder query = this.run(qs);
		
		result &= query.must().size() == 0;
		result &= query.mustNot().size() == 1;
		result &= query.should().size() == 0;
		result &= query.mustNot().get(0) instanceof WildcardQueryBuilder;
		result &= ((WildcardQueryBuilder)query.mustNot().get(0)).fieldName().equals("lid");
		result &= ((WildcardQueryBuilder)query.mustNot().get(0)).value().equals("pdart14_meap?");
		System.out.println((result ? "success" : "FAILURE") + " - "  + qs);
		return result;
	}

	private boolean verify_03()
	{
		boolean result = true;
		String qs = "lid eq \"*pdart14_meap?\"";
		BoolQueryBuilder query = this.run(qs);
		
		result &= query.must().size() == 1;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof MatchQueryBuilder;
		result &= ((MatchQueryBuilder)query.must().get(0)).fieldName().equals("lid");
		result &= ((MatchQueryBuilder)query.must().get(0)).value().equals("\"*pdart14_meap?\"");
		System.out.println((result ? "success" : "FAILURE") + " - "  + qs);
		return result;
	}

	private boolean verify_04()
	{
		boolean result = true;
		String qs = "( lid eq *pdart14_meap* )";
		BoolQueryBuilder query = this.run(qs);
		
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
		System.out.println((result ? "success" : "FAILURE") + " - "  + qs);
		return result;
	}

	private boolean verify_05()
	{
		boolean result = true;
		String qs = "( timestamp gt 12 and timestamp lt 27 )";
		BoolQueryBuilder query = this.run(qs);

		result &= query.must().size() == 1;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof BoolQueryBuilder;
		query = (BoolQueryBuilder)query.must().get(0);
		result &= query.must().size() == 2;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof RangeQueryBuilder;
		result &= query.must().get(1) instanceof RangeQueryBuilder;
		result &= ((RangeQueryBuilder)query.must().get(0)).fieldName().equals("timestamp");
		result &= ((RangeQueryBuilder)query.must().get(0)).from().equals("12");
		result &= ((RangeQueryBuilder)query.must().get(0)).to() == null;
		result &= !((RangeQueryBuilder)query.must().get(0)).includeLower();
		result &= ((RangeQueryBuilder)query.must().get(0)).includeUpper();
		result &= ((RangeQueryBuilder)query.must().get(1)).fieldName().equals("timestamp");
		result &= ((RangeQueryBuilder)query.must().get(1)).from() == null;
		result &= ((RangeQueryBuilder)query.must().get(1)).to().equals("27");
		result &= ((RangeQueryBuilder)query.must().get(1)).includeLower();
		result &= !((RangeQueryBuilder)query.must().get(1)).includeUpper();
		System.out.println((result ? "success" : "FAILURE") + " - "  + qs);
		return result;
	}

	private boolean verify_06()
	{
		boolean result = true;
		String qs = "( timestamp ge 12 and timestamp le 27 )";
		BoolQueryBuilder query = this.run(qs);

		result &= query.must().size() == 1;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof BoolQueryBuilder;
		query = (BoolQueryBuilder)query.must().get(0);
		result &= query.must().size() == 2;
		result &= query.mustNot().size() == 0;
		result &= query.should().size() == 0;
		result &= query.must().get(0) instanceof RangeQueryBuilder;
		result &= query.must().get(1) instanceof RangeQueryBuilder;
		result &= ((RangeQueryBuilder)query.must().get(0)).fieldName().equals("timestamp");
		result &= ((RangeQueryBuilder)query.must().get(0)).from().equals("12");
		result &= ((RangeQueryBuilder)query.must().get(0)).to() == null;
		result &= ((RangeQueryBuilder)query.must().get(0)).includeLower();
		result &= ((RangeQueryBuilder)query.must().get(0)).includeUpper();
		result &= ((RangeQueryBuilder)query.must().get(1)).fieldName().equals("timestamp");
		result &= ((RangeQueryBuilder)query.must().get(1)).from() == null;
		result &= ((RangeQueryBuilder)query.must().get(1)).to().equals("27");
		result &= ((RangeQueryBuilder)query.must().get(1)).includeLower();
		result &= ((RangeQueryBuilder)query.must().get(1)).includeUpper();
		System.out.println((result ? "success" : "FAILURE") + " - "  + qs);
		return result;
	}

	private boolean verify_99()
	{
		boolean result = true;
		String qs = "";
		BoolQueryBuilder query = this.run(qs);
		System.out.println((result ? "success" : "FAILURE") + " - "  + qs);
		return result;
	}
}
