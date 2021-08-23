package gov.nasa.pds.api.engineering.elasticsearch;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import gov.nasa.pds.api.engineering.elasticsearch.business.ProductQueryBuilderUtil;


public class Pds4JsonSearchRequestBuilder
{
    private static final String[] PDS4_JSON_PRODUCT_FIELDS = { 
            // JSON BLOB
            "ops:Label_File_Info/ops:json_blob",
            // Label Metadata
            "ops:Label_File_Info/ops:file_name",
            "ops:Label_File_Info/ops:creation_date_time",
            "ops:Label_File_Info/ops:file_ref",
            "ops:Label_File_Info/ops:file_size",
            "ops:Label_File_Info/ops:md5_checksum",
            // File Metadata
            "ops:Data_File_Info/ops:creation_date_time",
            "ops:Data_File_Info/ops:file_ref",
            "ops:Data_File_Info/ops:file_name",
            "ops:Data_File_Info/ops:file_size",
            "ops:Data_File_Info/ops:md5_checksum",
            "ops:Data_File_Info/ops:mime_type",
            // Node Name
            "ops:Harvest_Info/ops:node_name"
        };

    private String registryIndex;
    private String registryRefIndex;
    private int timeOutSeconds;

    /**
     * Constructor
     * @param registryIndex Elasticsearch registry index
     * @param registryRefindex Elasticsearch registry refs index
     * @param timeOutSeconds Elasticsearch request timeout
     */
    public Pds4JsonSearchRequestBuilder(String registryIndex, String registryRefindex, int timeOutSeconds) 
    {
        this.registryIndex = registryIndex;
        this.registryRefIndex = registryRefindex;
        this.timeOutSeconds = timeOutSeconds;
    }
    
    
    /**
     * Default construcotr
     */
    public Pds4JsonSearchRequestBuilder() 
    {
        this("registry", "registry-refs", 60);
    }

    
    /**
     * Create Elasticsearch request to fetch product by LIDVID. 
     * Get data required to represent the product in "pds4+json" format.
     * @param lidvid LIDVID of a product
     * @return Elasticsearch request
     */
    public GetRequest getProductRequest(String lidvid)
    {
        GetRequest getProductRequest = new GetRequest(this.registryIndex, lidvid);
        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, PDS4_JSON_PRODUCT_FIELDS, null);
        getProductRequest.fetchSourceContext(fetchSourceContext);
        return getProductRequest;
    }


    /**
     * Create Elasticsearch request to find products by PDS query or keywords
     * @param queryString PDS Query string
     * @param keyword Lucene query
     * @param fields fields to return
     * @param start used to paginate results
     * @param limit used to paginate results
     * @param presetCriteria used to select only bundles or collections
     * @return Elasticsearch request
     */
    public SearchRequest getSearchProductsRequest(String queryString, String keyword, List<String> fields, 
            int start, int limit, Map<String, String> presetCriteria)
    {
        QueryBuilder query = null;
        
        // "keyword" parameter provided. Run full-text query.
        if(keyword != null && !keyword.isBlank())
        {
            query = ProductQueryBuilderUtil.createKeywordQuery(keyword, presetCriteria);
        }
        // Run PDS query language ("q" parameter) query
        else
        {
            query = ProductQueryBuilderUtil.createPqlQuery(queryString, null, presetCriteria);
        }
        
        SearchRequest searchRequest = ProductQueryBuilderUtil.createSearchRequest(query, start, limit, 
                PDS4_JSON_PRODUCT_FIELDS, null, this.registryIndex, this.timeOutSeconds);

        return searchRequest;
    }

}
