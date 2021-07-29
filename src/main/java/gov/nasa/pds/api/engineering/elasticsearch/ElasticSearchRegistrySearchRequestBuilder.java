package gov.nasa.pds.api.engineering.elasticsearch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.lang.Math;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.common.unit.TimeValue;

import gov.nasa.pds.api.engineering.lexer.SearchLexer;
import gov.nasa.pds.api.engineering.lexer.SearchParser;
import gov.nasa.pds.api.engineering.controllers.ProductsRequest;
import gov.nasa.pds.api.engineering.elasticsearch.business.CollectionProductRefBusinessObject;
import gov.nasa.pds.api.engineering.elasticsearch.entities.EntityProduct;
import gov.nasa.pds.api.engineering.elasticsearch.entities.EntitytProductWithBlob;


public class ElasticSearchRegistrySearchRequestBuilder
{

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchRegistrySearchRequestBuilder.class);

    private String registryIndex;
    private String registryRefIndex;
    private int timeOutSeconds;

    public ElasticSearchRegistrySearchRequestBuilder(String registryIndex, String registryRefindex, int timeOutSeconds)
    {

        this.registryIndex = registryIndex;
        this.registryRefIndex = registryRefindex;
        this.timeOutSeconds = timeOutSeconds;

    }

    public ElasticSearchRegistrySearchRequestBuilder()
    {

        this.registryIndex = "registry";
        this.registryRefIndex = "registry-refs";
        this.timeOutSeconds = 60;

    }

    public SearchRequest getSearchProductRefsFromCollectionLidVid(String lidvid, int start, int limit)
    {
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("collection_lidvid", lidvid);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        int productRefStart = (int) Math
                .floor(start / (float) CollectionProductRefBusinessObject.PRODUCT_REFERENCES_BATCH_SIZE);
        int productRefLimit = (int) Math
                .ceil(limit / (float) CollectionProductRefBusinessObject.PRODUCT_REFERENCES_BATCH_SIZE);
        log.debug("Request product reference documents from " + Integer.toString(productRefStart) + " for size "
                + Integer.toString(productRefLimit));
        searchSourceBuilder.query(matchQueryBuilder).from(productRefStart).size(productRefLimit);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(this.registryRefIndex);

        log.debug("search product ref request :" + searchRequest.toString());

        return searchRequest;

    }

    public SearchRequest getSearchProductRequestHasLidVidPrefix(String lidvid)
    {
        PrefixQueryBuilder prefixQueryBuilder = QueryBuilders.prefixQuery("lidvid", lidvid);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(prefixQueryBuilder);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(this.registryIndex);

        return searchRequest;

    }

    private BoolQueryBuilder parseQueryString(String queryString)
    {
        CodePointCharStream input = CharStreams.fromString(queryString);
        SearchLexer lex = new SearchLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);

        SearchParser par = new SearchParser(tokens);
        par.setErrorHandler(new BailErrorStrategy());
        ParseTree tree = par.query();

        ElasticSearchRegistrySearchRequestBuilder.log.info(tree.toStringTree(par));

        // Walk it and attach our listener
        ParseTreeWalker walker = new ParseTreeWalker();
        Antlr4SearchListener listener = new Antlr4SearchListener();
        walker.walk(listener, tree);

        return listener.getBoolQuery();

    }

    private BoolQueryBuilder parseFields(List<String> fields)
    {
        BoolQueryBuilder fieldsBoolQuery = QueryBuilders.boolQuery();
        String esField;
        ExistsQueryBuilder existsQueryBuilder;
        for (String field : fields)
        {
            esField = ElasticSearchUtil.jsonPropertyToElasticProperty(field);
            existsQueryBuilder = QueryBuilders.existsQuery(esField);
            fieldsBoolQuery.should(existsQueryBuilder);
        }
        fieldsBoolQuery.minimumShouldMatch(1);

        return fieldsBoolQuery;
    }

//	public SearchRequest getSearchProductsRequest(String queryString, int start, int limit, Map<String,String> presetCriteria) {
//		return getSearchProductsRequest(queryString, null, start, limit, presetCriteria);
//	}

    public GetRequest getGetProductRequest(String lidvid, boolean withXMLBlob)
    {

        GetRequest getProductRequest = new GetRequest(this.registryIndex, lidvid);

        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, null, withXMLBlob ? null : new String[]
        {
                EntitytProductWithBlob.BLOB_PROPERTY
        });

        getProductRequest.fetchSourceContext(fetchSourceContext);

        return getProductRequest;

    }

    public GetRequest getGetProductRequest(String lidvid)
    {

        return this.getGetProductRequest(lidvid, false);

    }

    public SearchRequest getSearchProductsRequest(ProductsRequest req, Map<String, String> presetCriteria)
    {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (req.q != null)
        {
            ElasticSearchRegistrySearchRequestBuilder.log.debug("q value: " + req.q);
            boolQuery = this.parseQueryString(req.q);
        }

        for (Map.Entry<String, String> e : presetCriteria.entrySet())
        {
            // example "product_class", "Product_Collection"
            boolQuery.must(QueryBuilders.termQuery(e.getKey(), e.getValue()));
        }

        String[] includedFields = null;
        if (req.fields != null)
        {
            boolQuery.must(this.parseFields(req.fields));
            includedFields = createIncludedFields(req.fields);
        }

        SearchRequest searchRequest = createSearchRequest(boolQuery, req.start, req.limit, includedFields);

        return searchRequest;
    }

    
    private SearchRequest createSearchRequest(QueryBuilder query, int from, int size, String[] includedFields)
    {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);

        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includedFields, new String[]
        {
                EntitytProductWithBlob.BLOB_PROPERTY
        });

        searchSourceBuilder.fetchSource(fetchSourceContext);

        searchSourceBuilder.timeout(new TimeValue(this.timeOutSeconds, TimeUnit.SECONDS));

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        searchRequest.indices(this.registryIndex);

        ElasticSearchRegistrySearchRequestBuilder.log.debug("request elasticSearch :" + searchRequest.toString());
    
        return searchRequest;
    }

    
    private String[] createIncludedFields(List<String> fields)
    {
        HashSet<String> esFields = new HashSet<String>(Arrays.asList(EntityProduct.JSON_PROPERTIES));
        for (int i = 0; i < fields.size(); i++)
        {
            String includedField = ElasticSearchUtil.jsonPropertyToElasticProperty((String) fields.get(i));
            ElasticSearchRegistrySearchRequestBuilder.log.debug("add field " + includedField + " to search");
            esFields.add(includedField);
        }

        String[] includedFields = esFields.toArray(new String[esFields.size()]);
        
        return includedFields;
    }
    
    
    public SearchRequest getSearchProductRequest(ProductsRequest req)
    {
        Map<String, String> presetCriteria = new HashMap<String, String>();
        return getSearchProductsRequest(req, presetCriteria);
    }

    
    public SearchRequest getSearchCollectionRequest(ProductsRequest req)
    {

        Map<String, String> presetCriteria = new HashMap<String, String>();
        presetCriteria.put("product_class", "Product_Collection");
        return getSearchProductsRequest(req, presetCriteria);

    }

}
