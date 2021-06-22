package gov.nasa.pds.api.engineering.controllers;


import gov.nasa.pds.api.base.BundlesApi;
import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchRegistrySearchRequestBuilder;
import gov.nasa.pds.api.engineering.elasticsearch.ElasticSearchUtil;
import gov.nasa.pds.api.engineering.elasticsearch.business.ProductBusinessObject;
import gov.nasa.pds.api.engineering.elasticsearch.entities.EntityProduct;
import gov.nasa.pds.model.Product;
import gov.nasa.pds.model.Products;
import gov.nasa.pds.model.Summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-02-16T16:35:42.434-08:00[America/Los_Angeles]")
@Controller
public class MyBundlesApiController extends MyProductsApiBareController implements BundlesApi {

    private static final Logger log = LoggerFactory.getLogger(MyBundlesApiController.class);


    @org.springframework.beans.factory.annotation.Autowired
    public MyBundlesApiController(ObjectMapper objectMapper, HttpServletRequest request) {
    	super(objectMapper, request);
    	
		this.presetCriteria.put("product_class", "Product_Bundle");
	
    }

    public ResponseEntity<Product> bundleByLidvid(@ApiParam(value = "lidvid (urn)",required=true) @PathVariable("lidvid") String lidvid)
    {
    	return this.getProductResponseEntity(lidvid);
    }

    public ResponseEntity<Products> getBundles(@ApiParam(value = "offset in matching result list, for pagination", defaultValue = "0") @Valid @RequestParam(value = "start", required = false, defaultValue="0") Integer start
,@ApiParam(value = "maximum number of matching results returned, for pagination", defaultValue = "100") @Valid @RequestParam(value = "limit", required = false, defaultValue="100") Integer limit
,@ApiParam(value = "search query, complex query uses eq,ne,gt,ge,lt,le,(,),not,and,or. Properties are named as in 'properties' attributes, literals are strings between \" or numbers. Detailed query specification is available at https://bit.ly/393i1af") @Valid @RequestParam(value = "q", required = false) String q
,@ApiParam(value = "returned fields, syntax field0,field1") @Valid @RequestParam(value = "fields", required = false) List<String> fields
,@ApiParam(value = "sort results, syntax asc(field0),desc(field1)") @Valid @RequestParam(value = "sort", required = false) List<String> sort
,@ApiParam(value = "only return the summary, useful to get the list of available properties", defaultValue = "false") @Valid @RequestParam(value = "only-summary", required = false, defaultValue="false") Boolean onlySummary
) {
    	return this.getProductsResponseEntity(q, start, limit, fields, sort, onlySummary);
    }
    
    
    public ResponseEntity<Products> collectionsOfABundle(@ApiParam(value = "lidvid (urn)",required=true) @PathVariable("lidvid") String lidvid
    		,@ApiParam(value = "offset in matching result list, for pagination", defaultValue = "0") @Valid @RequestParam(value = "start", required = false, defaultValue="0") Integer start
    		,@ApiParam(value = "maximum number of matching results returned, for pagination", defaultValue = "100") @Valid @RequestParam(value = "limit", required = false, defaultValue="100") Integer limit
    		,@ApiParam(value = "returned fields, syntax field0,field1") @Valid @RequestParam(value = "fields", required = false) List<String> fields
    		,@ApiParam(value = "sort results, syntax asc(field0),desc(field1)") @Valid @RequestParam(value = "sort", required = false) List<String> sort
    		,@ApiParam(value = "only return the summary, useful to get the list of available properties", defaultValue = "false") @Valid @RequestParam(value = "only-summary", required = false, defaultValue="false") Boolean onlySummary

    		
    		) {
    		return this.getBundlesCollections(lidvid, start, limit, fields, sort, onlySummary);
    		           		    }

 	private List<String> getCollectionChildren (String lidvid) throws IOException
    {
    	List<String> collectionLIDVIDs = new ArrayList<String>();

    	for (Map<String,Object> bundle : ElasticSearchUtil.collate(this.esRegistryConnection.getRestHighLevelClient(),
				ElasticSearchRegistrySearchRequestBuilder.getQueryFieldFromLidvid(lidvid, "ref_lid_collection",
						this.esRegistryConnection.getRegistryIndex())))
		{
			if (bundle.get("ref_lid_collection") instanceof String)
			{ collectionLIDVIDs.add(this.productBO.getLatestLidVidFromLid(bundle.get("ref_lid_collection").toString())); }
			else
			{
				@SuppressWarnings("unchecked")
				List<String> clids = (List<String>)bundle.get("ref_lid_collection");
				for (String clid : clids)
				{ collectionLIDVIDs.add(this.productBO.getLatestLidVidFromLid(clid)); }
			}
		}
    	return collectionLIDVIDs;
    }
    
    private Products getCollectionChildren(String lidvid, int start, int limit, List<String> fields, List<String> sort, boolean onlySummary) throws IOException
    {
		if (!lidvid.contains("::") && !lidvid.endsWith(":")) lidvid = this.productBO.getLatestLidVidFromLid(lidvid);
    	MyBundlesApiController.log.info("request bundle lidvid, collections children: " + lidvid);

    	HashSet<String> uniqueProperties = new HashSet<String>();
    	List<String> clidvids = this.getCollectionChildren(lidvid);
    	Products products = new Products();
      	Summary summary = new Summary();

    	if (sort == null) { sort = Arrays.asList(); }

    	summary.setStart(start);
    	summary.setLimit(limit);
    	summary.setSort(sort);
    	products.setSummary(summary);

    	if (0 < clidvids.size() && start < clidvids.size())
    	{
    		this.fillProductsFromLidvids(products, uniqueProperties, clidvids, fields, start, limit, onlySummary);
    	}

    	summary.setProperties(new ArrayList<String>(uniqueProperties));
    	return products;	
    }

    private ResponseEntity<Products> getBundlesCollections(String lidvid, int start, int limit, List<String> fields, List<String> sort, boolean onlySummary) {
		 String accept = this.request.getHeader("Accept");
		 MyBundlesApiController.log.info("accept value is " + accept);
		 if ((accept != null 
		 		&& (accept.contains("application/json") 
		 				|| accept.contains("text/html")
		 				|| accept.contains("application/xml")
		 				|| accept.contains("*/*")))
		 	|| (accept == null)) {
		 	
		 	try {
		    	
		 	
		 		Products products = this.getCollectionChildren(lidvid, start, limit, fields, sort, onlySummary);
		    	
		    	return new ResponseEntity<Products>(products, HttpStatus.OK);
		    	
		  } catch (IOException e) {
		       log.error("Couldn't serialize response for content type " + accept, e);
		       return new ResponseEntity<Products>(HttpStatus.INTERNAL_SERVER_ERROR);
		  }
		     
		 }
		 else return new ResponseEntity<Products>(HttpStatus.NOT_IMPLEMENTED);
    }

	@Override
	public ResponseEntity<Products> productsOfABundle(String lidvid, @Valid Integer start, @Valid Integer limit,
			@Valid List<String> fields, @Valid List<String> sort, @Valid Boolean onlySummary) {
		 String accept = this.request.getHeader("Accept");
		 MyBundlesApiController.log.info("accept value is " + accept);
		 if ((accept != null 
		 		&& (accept.contains("application/json") 
		 				|| accept.contains("text/html")
		 				|| accept.contains("application/xml")
		 				|| accept.contains("*/*")))
		 	|| (accept == null)) {
		 	
		 	try {
		    	
		 	
		 		Products products = this.getProductChildren(lidvid, start, limit, fields, sort, onlySummary);
		    	
		    	return new ResponseEntity<Products>(products, HttpStatus.OK);
		    	
		  } catch (IOException e) {
		       log.error("Couldn't serialize response for content type " + accept, e);
		       return new ResponseEntity<Products>(HttpStatus.INTERNAL_SERVER_ERROR);
		  }
		     
		 }
		 else return new ResponseEntity<Products>(HttpStatus.NOT_IMPLEMENTED);
	}
	private Products getProductChildren(String lidvid, int start, int limit, List<String> fields, List<String> sort, boolean onlySummary) throws IOException
	
    {
    	
    	if (!lidvid.contains("::")) lidvid = this.productBO.getLatestLidVidFromLid(lidvid);
    	
    	MyBundlesApiController.log.info("request bundle lidvid, children of products: " + lidvid);
    	HashSet<String> uniqueProperties = new HashSet<String>();
    	List<String> clidvids = this.getCollectionChildren(lidvid);
    	List<String> plidvids = new ArrayList<String>();    		
    	Products products = new Products();
      	Summary summary = new Summary();

    	if (sort == null) { sort = Arrays.asList(); }

    	summary.setStart(start);
    	summary.setLimit(limit);
    	summary.setSort(sort);
    	products.setSummary(summary);
    	MyBundlesApiController.log.info("found " + Integer.toString(clidvids.size()) + " collections in this bundle");

    	if (0 < clidvids.size())
    	{
    		for (Map<String,Object> kvp : ElasticSearchUtil.collate(this.esRegistryConnection.getRestHighLevelClient(),
    				ElasticSearchRegistrySearchRequestBuilder.getQueryFieldFromKVP("collection_lidvid", clidvids, "product_lidvid",
    						this.esRegistryConnection.getRegistryRefIndex())))
    		{
    			if (kvp.get("product_lidvid") instanceof String)
    			{ plidvids.add(this.productBO.getLatestLidVidFromLid(kvp.get("product_lidvid").toString())); }
    			else
    			{
    				@SuppressWarnings("unchecked")
    				List<String> clids = (List<String>)kvp.get("product_lidvid");
    				for (String clid : clids)
    				{ plidvids.add(this.productBO.getLatestLidVidFromLid(clid)); }
    			}
    		}
    	}

    	MyBundlesApiController.log.info("found " + Integer.toString(plidvids.size()) + " products in this bundle");

    	if (0 < plidvids.size() && start < plidvids.size())
    	{
    		this.fillProductsFromLidvids(products, uniqueProperties, plidvids, fields, start, limit, onlySummary);
    	}

    	summary.setProperties(new ArrayList<String>(uniqueProperties));
    	return products;	
    }
}
