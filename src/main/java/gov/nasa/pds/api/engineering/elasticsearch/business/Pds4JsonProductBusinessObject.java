package gov.nasa.pds.api.engineering.elasticsearch.business;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchRegistryConnection;
import gov.nasa.pds.api.engineering.elasticsearch.Pds4JsonSearchRequestBuilder;
import gov.nasa.pds.model.Pds4Product;
import gov.nasa.pds.model.Pds4Products;


public class Pds4JsonProductBusinessObject
{
    private ElasticSearchRegistryConnection elasticSearchConnection;
    private Pds4JsonSearchRequestBuilder searchRequestBuilder;

    
    public Pds4JsonProductBusinessObject(ElasticSearchRegistryConnection esRegistryConnection)
    {
        this.elasticSearchConnection = esRegistryConnection;

        this.searchRequestBuilder = new Pds4JsonSearchRequestBuilder(
                this.elasticSearchConnection.getRegistryIndex(), 
                this.elasticSearchConnection.getRegistryRefIndex(),
                this.elasticSearchConnection.getTimeOutSeconds());
    }

    
    public Pds4Product getProduct(String lidvid) throws IOException 
    {
        GetRequest req = this.searchRequestBuilder.getProductRequest(lidvid);
        RestHighLevelClient client = this.elasticSearchConnection.getRestHighLevelClient();           
        GetResponse resp = client.get(req, RequestOptions.DEFAULT);
        
        if(!resp.isExists())
        {
            return null;
        }

        Map<String, Object> fieldMap = resp.getSourceAsMap();
        Pds4Product prod = Pds4JsonProductFactory.createProduct(lidvid, fieldMap);
        return prod;
    }

    
    public Pds4Products getProducts(String q, String keyword, 
            int start, int limit, List<String> fields, List<String> sort, boolean onlySummary) throws IOException 
    {
        return null;
    }

}
