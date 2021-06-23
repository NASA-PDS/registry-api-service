package gov.nasa.pds.api.engineering.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchRegistryConnection;
import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchRegistrySearchRequestBuilder;
import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchUtil;
import gov.nasa.pds.api.engineering.elasticsearch.business.ProductBusinessObject;
import gov.nasa.pds.api.engineering.elasticsearch.entities.EntityProduct;

import gov.nasa.pds.api.model.ProductWithXmlLabel;
import gov.nasa.pds.model.Product;
import gov.nasa.pds.model.PropertyValues;
import gov.nasa.pds.model.Products;
import gov.nasa.pds.model.Summary;

@Component
public class MyProductsApiBareController {
	
	private static final Logger log = LoggerFactory.getLogger(MyProductsApiBareController.class);  
	
    protected final ObjectMapper objectMapper;

    protected final HttpServletRequest request;   

	protected Map<String, String> presetCriteria = new HashMap<String, String>();
	
	// TODO remove and replace by BusinessObjects 
	@Autowired
	ElasticSearchRegistryConnection esRegistryConnection;
	
	@Autowired
	protected ProductBusinessObject productBO;

	@Autowired
	ElasticSearchRegistrySearchRequestBuilder searchRequestBuilder;
	
	
    public MyProductsApiBareController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
         
    }
    
    
    protected void fillProductsFromLidvids (Products products, HashSet<String> uniqueProperties, List<String> lidvids, List<String> fields, int start, int limit, boolean onlySummary) throws IOException
    {
    	for (Map<String,Object> kvp : ElasticSearchUtil.collate(this.esRegistryConnection.getRestHighLevelClient(),
				ElasticSearchRegistrySearchRequestBuilder.getQueryFieldsFromKVP("lidvid",
						lidvids.subList(start, start+limit < lidvids.size() ? start+limit : lidvids.size()),
						fields, this.esRegistryConnection.getRegistryIndex())))
		{
			uniqueProperties.addAll(kvp.keySet());

			if (!onlySummary)
			{
				products.addDataItem(ElasticSearchUtil.ESentityProductToAPIProduct(objectMapper.convertValue(kvp, EntityProduct.class)));
				products.getData().get(products.getData().size()-1).setProperties(ProductBusinessObject.getFilteredProperties(kvp, null, null));
			}
		}

    }

    protected void fillProductsFromParents (Products products, HashSet<String> uniqueProperties, List<Map<String,Object>> results, boolean onlySummary) throws IOException
    {
    	for (Map<String,Object> kvp : results)
    	{
			uniqueProperties.addAll(kvp.keySet());

			if (!onlySummary)
			{
				products.addDataItem(ElasticSearchUtil.ESentityProductToAPIProduct(objectMapper.convertValue(kvp, EntityProduct.class)));
				products.getData().get(products.getData().size()-1).setProperties(ProductBusinessObject.getFilteredProperties(kvp, null, null));
			}
    	}
    }

    protected Products getProducts(String q, int start, int limit, List<String> fields, List<String> sort, boolean onlySummary) throws IOException {
    	

    		        	
    	SearchRequest searchRequest = this.searchRequestBuilder.getSearchProductsRequest(q, fields, start, limit, this.presetCriteria);
    	
    	SearchResponse searchResponse = this.esRegistryConnection.getRestHighLevelClient().search(searchRequest, 
    			RequestOptions.DEFAULT);
    	
    	Products products = new Products();
    	
    	HashSet<String> uniqueProperties = new HashSet<String>();
    	
      	Summary summary = new Summary();
    	

      	summary.setQ((q != null)?q:"" );
    	summary.setStart(start);
    	summary.setLimit(limit);
    	
    	if (sort == null) {
    		sort = Arrays.asList();
    	}	
    	summary.setSort(sort);
    	
    	products.setSummary(summary);
    	
    	if (searchResponse != null) {
    		
    		for (SearchHit searchHit : searchResponse.getHits()) {
    	        Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
    	        
    	        Map<String, PropertyValues> filteredMapJsonProperties = ProductBusinessObject.getFilteredProperties(
    	        		sourceAsMap, 
    	        		fields, 
    	        		null
    	        		);
    	        
    	        uniqueProperties.addAll(filteredMapJsonProperties.keySet());

    	        if (!onlySummary) {
        	        EntityProduct entityProduct = objectMapper.convertValue(sourceAsMap, EntityProduct.class);
        	        Product product = ElasticSearchUtil.ESentityProductToAPIProduct(entityProduct);
        	        product.setProperties(filteredMapJsonProperties);
        	        products.addDataItem(product);
    	        }
    	        
    	    }
     
            
    	}
    	
    	
    	summary.setProperties(new ArrayList<String>(uniqueProperties));
    	
    	return products;
    }
    
 

    
    
    protected ResponseEntity<Products> getProductsResponseEntity(String q, int start, int limit, List<String> fields, List<String> sort, boolean onlySummary) {
        String accept = this.request.getHeader("Accept");
        log.info("accept value is " + accept);
        if ((accept != null 
        		&& (accept.contains("application/json") 
        				|| accept.contains("text/html")
        				|| accept.contains("application/xml")
        				|| accept.contains("*/*")))
        	|| (accept == null)) {
        	
        	try {
	        	
        	
        		Products products = this.getProducts(q, start, limit, fields, sort, onlySummary);
	        	
	        	return new ResponseEntity<Products>(products, HttpStatus.OK);
	        	
    	  } catch (IOException e) {
              log.error("Couldn't serialize response for content type " + accept, e);
              return new ResponseEntity<Products>(HttpStatus.INTERNAL_SERVER_ERROR);
          }
        	catch (ParseCancellationException pce) {
        		log.error("Could not parse the query string: " + q);
        		return new ResponseEntity<Products>(HttpStatus.BAD_REQUEST);
        	}            
        }
        else return new ResponseEntity<Products>(HttpStatus.NOT_IMPLEMENTED);
    }
    
    
    protected ResponseEntity<Product> getProductResponseEntity(String lidvid){
    	String accept = request.getHeader("Accept");
        if ((accept != null) 
        		&& (accept.contains("application/json")
				|| accept.contains("text/html")
				|| accept.contains("*/*")
				|| accept.contains("application/xml")
				|| accept.contains("application/pds4+xml"))) {
        	
            try {
            	
            	if (!lidvid.contains("::")) lidvid = this.productBO.getLatestLidVidFromLid(lidvid);
            	
               	ProductWithXmlLabel product = this.productBO.getProductWithXml(lidvid);
            	
               	if (product != null) {	
                   	
	        		return new ResponseEntity<Product>(product, HttpStatus.OK);
	        	}		        	
	        	else {
	        		// TODO send 302 redirection to a different server if one exists
	        		return new ResponseEntity<Product>(HttpStatus.NOT_FOUND);
	        	}
	        		

            } catch (IOException e) {
                log.error("Couldn't get or serialize response for content type " + accept, e);
                return new ResponseEntity<Product>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Product>(HttpStatus.NOT_IMPLEMENTED);
    }
    	

}
