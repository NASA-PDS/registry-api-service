package tt;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;


public class TestQuery1
{

    public static void main(String[] args) throws Exception
    {
        RestHighLevelClient client = null;

        try
        {
            client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
            
            QueryStringQueryBuilder luceneQuery = QueryBuilders.queryStringQuery("isis2");
            luceneQuery.field("title");
            luceneQuery.field("pds:Bundle/pds:description");
            
            SearchSourceBuilder srcBuilder = new SearchSourceBuilder();
            srcBuilder.query(luceneQuery);

            SearchRequest searchRequest = new SearchRequest();
            searchRequest.source(srcBuilder);
            searchRequest.indices("registry");
            
            SearchResponse resp = client.search(searchRequest, RequestOptions.DEFAULT);
            
            System.out.println();
            System.out.println("**********************************");
            
            for(SearchHit hit : resp.getHits())
            {
                System.out.println(hit.getId());
            }
            
            System.out.println("**********************************");
            System.out.println();
        }
        finally
        {
            if(client != null) client.close();
        }
        
    }

}
