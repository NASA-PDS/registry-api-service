package gov.nasa.pds.api.engineering.elasticsearch.business;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;

import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchRegistryConnection;
import gov.nasa.pds.api.engineering.elasticsearch.Pds4JsonSearchRequestBuilder;
import gov.nasa.pds.model.Pds4Product;
import gov.nasa.pds.model.Pds4Products;
import gov.nasa.pds.model.Summary;


public class Pds4JsonProductService
{
    private ElasticSearchRegistryConnection esConnection;
    private Pds4JsonSearchRequestBuilder searchRequestBuilder;

    
    public Pds4JsonProductService(ElasticSearchRegistryConnection esRegistryConnection)
    {
        this.esConnection = esRegistryConnection;

        this.searchRequestBuilder = new Pds4JsonSearchRequestBuilder(
                this.esConnection.getRegistryIndex(), 
                this.esConnection.getRegistryRefIndex(),
                this.esConnection.getTimeOutSeconds());
    }

    
    public Pds4Product getProduct(String lidvid) throws IOException 
    {
        GetRequest req = this.searchRequestBuilder.getProductRequest(lidvid);
        RestHighLevelClient client = this.esConnection.getRestHighLevelClient();           
        GetResponse resp = client.get(req, RequestOptions.DEFAULT);
        
        if(!resp.isExists())
        {
            return null;
        }

        Map<String, Object> fieldMap = resp.getSourceAsMap();
        Pds4Product prod = Pds4JsonProductFactory.createProduct(lidvid, fieldMap);
        return prod;
    }

    
    public Pds4Products getProducts(String q, String keyword, int start, int limit, List<String> fields, 
            List<String> sort, boolean onlySummary, Map<String, String> presetCriteria) throws IOException 
    {
        SearchRequest searchRequest = searchRequestBuilder.getSearchProductsRequest(q, keyword, fields, start, limit,
                presetCriteria);

        SearchResponse searchResponse = esConnection.getRestHighLevelClient().search(searchRequest,
                RequestOptions.DEFAULT);

        Pds4Products products = new Pds4Products();

        // Summary
        Summary summary = new Summary();
        summary.setQ(q);
        summary.setStart(start);
        summary.setLimit(limit);
        summary.setSort(sort);
        products.setSummary(summary);
        
        if(searchResponse == null) return products;
        
        // Products
        for(SearchHit hit : searchResponse.getHits()) 
        {
            String id = hit.getId();
            Map<String, Object> fieldMap = hit.getSourceAsMap();
            Pds4Product prod = Pds4JsonProductFactory.createProduct(id, fieldMap);
            products.addDataItem(prod);
        }

        return products;
    }

}
