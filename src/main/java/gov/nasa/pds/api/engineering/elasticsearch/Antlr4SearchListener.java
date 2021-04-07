package gov.nasa.pds.api.engineering.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import gov.nasa.pds.api.engineering.lexer.SearchBaseListener;
import gov.nasa.pds.api.engineering.lexer.SearchParser;

public class Antlr4SearchListener extends SearchBaseListener
{
	enum conjunctions { AND, OR };
	enum operation { eq, ge, gt, le, lt, ne };
	
	private class QueryError extends RuntimeException{private static final long serialVersionUID = 145252264968900221L;};

	private static final Logger log = LoggerFactory.getLogger(Antlr4SearchListener.class);
	
	private BoolQueryBuilder query;
	private boolean wildcard = false;
	final private Deque<conjunctions> conjunction = new ArrayDeque<conjunctions>(); 
	final private Deque<BoolQueryBuilder> stack_queries = new ArrayDeque<BoolQueryBuilder>();
	final private Deque<List<QueryBuilder>> stack_musts = new ArrayDeque<List<QueryBuilder>>();
	final private Deque<List<QueryBuilder>> stack_nots = new ArrayDeque<List<QueryBuilder>>();
	final private Deque<List<QueryBuilder>> stack_shoulds = new ArrayDeque<List<QueryBuilder>>();
	private List<QueryBuilder> musts = new ArrayList<QueryBuilder>();
	private List<QueryBuilder> nots = new ArrayList<QueryBuilder>();
	private List<QueryBuilder> shoulds = new ArrayList<QueryBuilder>();
	private operation operator = null;
	
	public Antlr4SearchListener(BoolQueryBuilder boolQuery)
	{	
		super();
		this.query = boolQuery;
	}
	
	
	 @Override
	 public void enterQuery(SearchParser.QueryContext ctx)
	 {
		 Antlr4SearchListener.log.debug("enterQuery: " + ctx.getText());
     }
	 
	 @Override
	 public void exitQuery(SearchParser.QueryContext ctx)
	 {
		 Antlr4SearchListener.log.debug("exitQuery: " + ctx.getText());
		 if (!this.stack_queries.isEmpty()) throw new QueryError(); // PANIC: unpaired parenthesis
		 if (!this.conjunction.isEmpty()) throw new QueryError(); // PANIC: AND/OR expression ended prematurely

		 for (QueryBuilder qb : musts) this.query.must(qb);
		 for (QueryBuilder qb : nots) this.query.mustNot(qb);
		 for (QueryBuilder qb : shoulds) this.query.should(qb);
	 }
	 
	 @Override
	 public void enterQueryTerm(SearchParser.QueryTermContext ctx)
	 {
		 Antlr4SearchListener.log.debug("enter queryterm: " + ctx.getText());
     }
	 
	 @Override
	 public void exitQueryTerm(SearchParser.QueryTermContext ctx)
	 {
		 Antlr4SearchListener.log.debug("exit queryterm: " + ctx.getText());
	 }

	 @Override
	 public void enterGroup(SearchParser.GroupContext ctx)
	 {
		 Antlr4SearchListener.log.debug("enter group: " + ctx.getText());
		 this.stack_queries.addLast(this.query);
		 this.stack_musts.add(this.musts);
		 this.stack_nots.add(this.nots);
		 this.stack_shoulds.add(this.shoulds);
		 this.musts = new ArrayList<QueryBuilder>();
		 this.nots = new ArrayList<QueryBuilder>();
		 this.shoulds = new ArrayList<QueryBuilder>();
		 this.query = new BoolQueryBuilder();
     }
	 
	 @Override
	 public void exitGroup(SearchParser.GroupContext ctx)
	 {
		 Antlr4SearchListener.log.debug("exit group: " + ctx.getText());
		 BoolQueryBuilder group = this.query;
		 List<QueryBuilder> musts = this.musts;
		 List<QueryBuilder> nots = this.nots;
		 List<QueryBuilder> shoulds = this.shoulds;

		 this.query = this.stack_queries.getLast();
		 this.musts = this.stack_musts.getLast();
		 this.nots = this.stack_nots.getLast();
		 this.shoulds = this.stack_shoulds.getLast();

		 for (QueryBuilder qb : musts) group.must(qb);
		 for (QueryBuilder qb : nots) group.mustNot(qb);
		 for (QueryBuilder qb : shoulds) group.should(qb);
		 
		 if (ctx.NOT() != null) this.nots.add(group);
		 else if (conjunction.isEmpty() || conjunction.peekLast() == conjunctions.AND) this.musts.add(group);
		 else this.shoulds.add(group);
	 }
	 
	 @Override
	 public void enterExpression(SearchParser.ExpressionContext ctx)
	 {
		 Antlr4SearchListener.log.debug("enter expression: " + ctx.getText());
	 }
	 
	 @Override
	 public void exitExpression(SearchParser.ExpressionContext ctx)
	 {
		 Antlr4SearchListener.log.debug("exit expression: " + ctx.getText());
		 this.conjunction.getLast();
	 }

	 @Override
	 public void enterAndStatement(SearchParser.AndStatementContext ctx)
	 {
		 Antlr4SearchListener.log.debug("enter andStatement: " + ctx.getText());
		 this.conjunction.addLast(conjunctions.AND);
	 }

	 @Override
	 public void enterOrStatement(SearchParser.OrStatementContext ctx)
	 {
		 Antlr4SearchListener.log.debug("enter orStatement: " + ctx.getText());
		 this.conjunction.addLast(conjunctions.OR);
	 }

	 @Override
	 public void exitOrStatement(SearchParser.OrStatementContext ctx)
	 {
		 Antlr4SearchListener.log.debug("exit orStatement: " + ctx.getText());
	 }

	 @Override
	 public void exitAndStatement(SearchParser.AndStatementContext ctx)
	 {
		 Antlr4SearchListener.log.debug("exit andStatement: " + ctx.getText());
	 }

	 @Override
	 public void enterComparison(SearchParser.ComparisonContext ctx)
	 {
		 Antlr4SearchListener.log.debug("enter comparison: " + ctx.getText());
		 
		 this.wildcard = ctx.VALUE() != null || ctx.VALUE().getSymbol().getText().contains("*") || ctx.VALUE().getSymbol().getText().contains("?");
 	 }

	@Override
	public void exitComparison(SearchParser.ComparisonContext ctx)
	{
		Antlr4SearchListener.log.debug("exit comparison: " + ctx.getText());
		String left = ctx.FIELD().getSymbol().getText(), right;
		QueryBuilder comparator = null;

		if (ctx.NUMBER() != null) right = ctx.NUMBER().getSymbol().getText();
		else if (ctx.STRINGVAL() != null) right = ctx.STRINGVAL().getSymbol().getText();
		else if (ctx.VALUE() != null) right = ctx.VALUE().getSymbol().getText();
		else throw new RuntimeException(); // PANIC: listener out of sync with the grammar

		if (this.operator == operation.eq || this.operator == operation.ne)
		{
			if (this.wildcard) comparator = new WildcardQueryBuilder(left, right);
			else comparator = new MatchQueryBuilder(right, left);
		}
		else
		{
			comparator = new RangeQueryBuilder(left);
			
			if (this.operator == operation.ge) ((RangeQueryBuilder)comparator).gte(right);
			else if (this.operator == operation.gt) ((RangeQueryBuilder)comparator).gt(right);
			else if (this.operator == operation.le) ((RangeQueryBuilder)comparator).lte(right);
			else if (this.operator == operation.lt) ((RangeQueryBuilder)comparator).lt(right);
			else throw new RuntimeException(); // PANIC: listener out of sync with the grammar
		}

		if (this.operator == operation.ne) this.nots.add(comparator);
		else if (this.conjunction.isEmpty() || this.conjunction.peekLast() == conjunctions.AND) this.musts.add(comparator);	
		else this.shoulds.add(comparator);
	}

	@Override
	public void enterOperator(SearchParser.OperatorContext ctx)
	{
		Antlr4SearchListener.log.debug("enter operator: " + ctx.getText());

		if (this.wildcard && ctx.EQ() == null && ctx.NE() == null) throw new QueryError();
		
		if (ctx.EQ() != null || ctx.NE() != null)
		{
			if (this.wildcard) this.operator = operation.eq;
		}
	}
	 
	 
	 public BoolQueryBuilder getBoolQuery()
	 {
		 return this.query;
	 }
}
