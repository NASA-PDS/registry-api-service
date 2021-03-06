package gov.nasa.pds.api.engineering.controllers;


import gov.nasa.pds.api.base.CollectionsApi;
import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchUtil;
import gov.nasa.pds.api.engineering.elasticsearch.business.CollectionProductRefBusinessObject;
import gov.nasa.pds.api.engineering.elasticsearch.business.CollectionProductRelationships;
import gov.nasa.pds.api.engineering.elasticsearch.entities.EntityProduct;
import gov.nasa.pds.api.engineering.elasticsearch.entities.EntitytProductWithBlob;
import gov.nasa.pds.api.engineering.exceptions.UnsupportedElasticSearchProperty;
import gov.nasa.pds.api.model.xml.ProductWithXmlLabel;
import gov.nasa.pds.api.model.xml.XMLMashallableProperyValue;
import gov.nasa.pds.model.Product;
import gov.nasa.pds.model.PropertyArrayValues;
import gov.nasa.pds.model.Products;
import gov.nasa.pds.model.Summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

import gov.nasa.pds.api.engineering.elasticsearch.business.LidVidNotFoundException;
import gov.nasa.pds.api.engineering.elasticsearch.business.ProductBusinessObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;



@Controller
public class MyCollectionsApiController extends MyProductsApiBareController implements CollectionsApi {

    private static final Logger log = LoggerFactory.getLogger(MyCollectionsApiController.class);
    
 
    public MyCollectionsApiController(ObjectMapper objectMapper, HttpServletRequest request) {
    	super(objectMapper, request);
    	
		this.presetCriteria.put("product_class", "Product_Collection");
	
    }
    
    
    public ResponseEntity<Product> collectionsByLidvid(@ApiParam(value = "lidvid (urn)",required=true) @PathVariable("lidvid") String lidvid) {
    	return this.getProductResponseEntity(lidvid);
    }



    public ResponseEntity<Products> getCollection(@ApiParam(value = "offset in matching result list, for pagination", defaultValue = "0") @Valid @RequestParam(value = "start", required = false, defaultValue="0") Integer start
    		,@ApiParam(value = "maximum number of matching results returned, for pagination", defaultValue = "100") @Valid @RequestParam(value = "limit", required = false, defaultValue="100") Integer limit
    		,@ApiParam(value = "search query, complex query uses eq,ne,gt,ge,lt,le,(,),not,and,or. Properties are named as in 'properties' attributes, literals are strings between \" or numbers. Detailed query specification is available at https://bit.ly/393i1af") @Valid @RequestParam(value = "q", required = false) String q
    		,@ApiParam(value = "returned fields, syntax field0,field1") @Valid @RequestParam(value = "fields", required = false) List<String> fields
    		,@ApiParam(value = "sort results, syntax asc(field0),desc(field1)") @Valid @RequestParam(value = "sort", required = false) List<String> sort
    		,@ApiParam(value = "only return the summary, useful to get the list of available properties", defaultValue = "false") @Valid @RequestParam(value = "only-summary", required = false, defaultValue="false") Boolean onlySummary
    		) {
    	
    	return this.getProductsResponseEntity(q, start, limit, fields, sort, onlySummary);
    }
    
    
    public ResponseEntity<Products> productsOfACollection(@ApiParam(value = "lidvid (urn)",required=true) @PathVariable("lidvid") String lidvid
    		,@ApiParam(value = "offset in matching result list, for pagination", defaultValue = "0") @Valid @RequestParam(value = "start", required = false, defaultValue="0") Integer start
    		,@ApiParam(value = "maximum number of matching results returned, for pagination", defaultValue = "100") @Valid @RequestParam(value = "limit", required = false, defaultValue="100") Integer limit
    		,@ApiParam(value = "returned fields, syntax field0,field1") @Valid @RequestParam(value = "fields", required = false) List<String> fields
    		,@ApiParam(value = "sort results, syntax asc(field0),desc(field1)") @Valid @RequestParam(value = "sort", required = false) List<String> sort
    		,@ApiParam(value = "only return the summary, useful to get the list of available properties", defaultValue = "false") @Valid @RequestParam(value = "only-summary", required = false, defaultValue="false") Boolean onlySummary
    		) {
    	
    	 MyCollectionsApiController.log.info("Get productsOfACollection");
    	
    	 String accept = this.request.getHeader("Accept");
    	 MyCollectionsApiController.log.info("accept value is " + accept);
		 if ((accept != null 
		 		&& (accept.contains("application/json") 
		 				|| accept.contains("text/html")
		 				|| accept.contains("application/xml")
		 				|| accept.contains("application/pds4+xml")
		 				|| accept.contains("*/*")))
		 	|| (accept == null)) {
		 	
		 	try {
		    	
		 	
		 		Products products = this.getProductChildren(lidvid, start, limit, fields, sort, onlySummary);
		    	
		 		/* REMOVED since it breaks the result when only-smmary argument is set to true
		 		if (products.getData() == null || products.getData().size() == 0)
		 			return new ResponseEntity<Products>(products, HttpStatus.NOT_FOUND);
		 		else
		 		*/
		 		
		 		return new ResponseEntity<Products>(products, HttpStatus.OK);
		  } catch (LidVidNotFoundException e) {
			  log.error("Couldn't find the lidvid " + e.getMessage());
			  return new ResponseEntity<Products>(HttpStatus.NOT_FOUND);
			  
		  } catch (IOException e) {
		       log.error("Couldn't serialize response for content type " + accept, e);
		       return new ResponseEntity<Products>(HttpStatus.INTERNAL_SERVER_ERROR);
		  }
		     
		 }
		 else return new ResponseEntity<Products>(HttpStatus.NOT_IMPLEMENTED);
    }
    		
	
    
    @SuppressWarnings("unchecked")
	private Products getProductChildren(String lidvid, int start, int limit, List<String> fields, List<String> sort, boolean onlySummary) throws IOException, LidVidNotFoundException {
  	
    	if (!lidvid.contains("::")) lidvid = this.productBO.getLatestLidVidFromLid(lidvid);
    	MyCollectionsApiController.log.info("request collection lidvid, collections children: " + lidvid);
         
    	try {
	    	Products products = new Products();
	    	
	    	HashSet<String> uniqueProperties = new HashSet<String>();
	    	
	      	Summary summary = new Summary();
	    	
	    	summary.setStart(start);
	    	summary.setLimit(limit);
	    	
	    	if (sort == null) {
	    		sort = Arrays.asList();
	    	}	
	    	summary.setSort(sort);
	    	
	    	products.setSummary(summary);
	    	
	    	CollectionProductRefBusinessObject  collectionProductRefBO = new CollectionProductRefBusinessObject(this.esRegistryConnection);
	    	CollectionProductRelationships collectionProductRelationships = collectionProductRefBO.getCollectionProductsIterable(lidvid, start, limit);
	    	
	    	for (EntityProduct eProd : collectionProductRelationships) {
				  if (eProd != null) {
		        	MyCollectionsApiController.log.info("request lidvdid: " + eProd.getLidVid() );
		        	
		    		Product product = ElasticSearchUtil.ESentityProductToAPIProduct(eProd, this.getBaseURL());
				    		
		    		Map<String, XMLMashallableProperyValue> filteredMapJsonProperties = ProductBusinessObject.getFilteredProperties(
		    				eProd.getProperties(), 
		    				fields,
		    				null);
		
		    		uniqueProperties.addAll(filteredMapJsonProperties.keySet());
		
		    		if (!onlySummary) {
		        		product.setProperties((Map<String, PropertyArrayValues>)(Map<String, ?>)filteredMapJsonProperties);
		        		
		        		products.addDataItem(product);
		    		}
				}
				  else {
					  MyCollectionsApiController.log.warn("Couldn't get one product child of collection " + lidvid + " in elasticSearch");
		    	      
				  }
	    	}
	                   	
	    			    	
	    	
	    	summary.setProperties(new ArrayList<String>(uniqueProperties));
	    	return products;
			
			
		} catch (IOException e) {
			MyCollectionsApiController.log.error("Couldn't get bundle " + lidvid + " from elasticSearch", e);
            throw(e);
		}
    	
    }


	@Override
	public ResponseEntity<Products> bundlesContainingCollection(String lidvid, @Valid Integer start, @Valid Integer limit,
			@Valid List<String> fields, @Valid List<String> sort, @Valid Boolean summaryOnly)
	{
		String accept = this.request.getHeader("Accept");
		MyCollectionsApiController.log.info("accept value is " + accept);

		if ((accept != null 
				&& (accept.contains("application/json") 
						|| accept.contains("text/html")
		 				|| accept.contains("application/xml")
		 				|| accept.contains("*/*")))
		 	|| (accept == null))
		{
			try
			{
		 		Products products = this.getContainingBundle(lidvid, start, limit, fields, sort, summaryOnly);		 		
		 		return new ResponseEntity<Products>(products, HttpStatus.OK);
			}
			catch (IOException e)
			{
				log.error("Couldn't serialize response for content type " + accept, e);
				return new ResponseEntity<Products>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		 }
		 else return new ResponseEntity<Products>(HttpStatus.NOT_IMPLEMENTED);
	}
    
    @SuppressWarnings("unchecked")
	private Products getContainingBundle(String lidvid, int start, int limit, List<String> fields, List<String> sort, boolean summaryOnly) throws IOException
    {
    		
    	if (!lidvid.contains("::")) lidvid = this.productBO.getLatestLidVidFromLid(lidvid);

    	MyCollectionsApiController.log.info("find all bundles containing the collection lidvid: " + lidvid);
    	MyCollectionsApiController.log.info("find all bundles containing the collection lid: " + lidvid.substring(0, lidvid.indexOf("::")));
    	HashSet<String> uniqueProperties = new HashSet<String>();
    	Products products = new Products();
    	SearchRequest request = new SearchRequest(this.esRegistryConnection.getRegistryIndex());
    	SearchResponse response;
    	SearchSourceBuilder builder = new SearchSourceBuilder();
      	Summary summary = new Summary();

    	if (sort == null) { sort = Arrays.asList(); }

    	summary.setStart(start);
    	summary.setLimit(limit);
    	summary.setSort(sort);
    	products.setSummary(summary);
    	builder.query(QueryBuilders.matchQuery("ref_lid_collection", lidvid.substring(0, lidvid.indexOf("::"))));
    	request.source(builder);
    	response = this.esRegistryConnection.getRestHighLevelClient().search(request,RequestOptions.DEFAULT);
    	
    	try {
	    	
	    	for (int i = start ; start < limit && i < response.getHits().getHits().length ; i++)
	    	{
		        Map<String, Object> sourceAsMap = response.getHits().getAt(i).getSourceAsMap();
		        Map<String, XMLMashallableProperyValue> filteredMapJsonProperties = ProductBusinessObject.getFilteredProperties(
		        		sourceAsMap, 
		        		fields,
		        		new ArrayList<String>(Arrays.asList(ElasticSearchUtil.elasticPropertyToJsonProperty(EntitytProductWithBlob.BLOB_PROPERTY)))
		        		);
		        
		        uniqueProperties.addAll(filteredMapJsonProperties.keySet());
	
		        if (!summaryOnly) {
	    	        EntityProduct entityProduct = objectMapper.convertValue(sourceAsMap, EntityProduct.class);

	    	        Product product = ElasticSearchUtil.ESentityProductToAPIProduct(entityProduct, this.getBaseURL());
					product.setProperties((Map<String, PropertyArrayValues>)(Map<String, ?>)filteredMapJsonProperties);

	    	        products.addDataItem(product);
		        }
	    	}
    	} catch (UnsupportedElasticSearchProperty e) {
    		log.error("This should never happen " + e.getMessage());
    	}
	    summary.setProperties(new ArrayList<String>(uniqueProperties));
    	return products;
    }
}