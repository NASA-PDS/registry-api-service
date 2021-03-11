package gov.nasa.pds.api.engineering.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import gov.nasa.pds.api.engineering.elasticsearch.entities.EntityProduct;
import gov.nasa.pds.api.engineering.exceptions.UnsupportedElasticSearchProperty;
import gov.nasa.pds.model.Metadata;
import gov.nasa.pds.api.model.ProductWithXmlLabel;
import gov.nasa.pds.model.Reference;

public class ElasticSearchUtil {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticSearchUtil.class);
    
	static public String jsonPropertyToElasticProperty(String jsonProperty) {
		return jsonProperty.replace(".", "/");
		
	}
	
	static private void append (ArrayList<Reference> to, String ID, String baseURL)
	{
		Reference reference = new Reference();
		reference.setId(ID + ".orex");
		reference.setHref(baseURL + "/products/" + reference.getId());
		to.add(reference);
	}
	
	static public String elasticPropertyToJsonProperty(String elasticProperty) throws UnsupportedElasticSearchProperty {
		   		
			return elasticProperty.replace('/', '.');
	 }
	
	

	static public Map<String, Object> elasticHashMapToJsonHashMap(Map<String, Object> sourceAsMap){
			 Map<String, Object> sourceAsMapJsonProperties = new HashMap<String, Object>();
			 Iterator<Entry<String, Object>> iterator = sourceAsMap.entrySet().iterator();
		     while (iterator.hasNext()) {
		    	 try {
	  	    	 Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
	  	    	 sourceAsMapJsonProperties.put(elasticPropertyToJsonProperty(entry.getKey()),
	                       entry.getValue());
		    	 } catch (UnsupportedElasticSearchProperty e) {
		    		 ElasticSearchUtil.log.warn(e.getMessage());
		    	 }
		     }
		     
		     return sourceAsMapJsonProperties;
	   }



	static public ProductWithXmlLabel ESentityProductToAPIProduct(EntityProduct ep) {
		ProductWithXmlLabel product = new ProductWithXmlLabel();
		product.setId(ep.getLidVid());
		product.setType(ep.getProductClass());
		
		String title = ep.getTitle();
		if (title != null) {
			product.setTitle(ep.getTitle());
		}
		
		String startDateTime = ep.getStartDateTime();
		if (startDateTime != null) {
			product.setStartDateTime(startDateTime);
		}
		
		String stopDateTime = ep.getStopDateTime();
		if (stopDateTime != null) {
			product.setStopDateTime(ep.getStopDateTime());
		}
		
		/*
		for (String reference_role: ep.PROCEDURE_REFERENCE_ROLES) {
			Reference observingSystemComponentRef = ep.geReference(reference_role);
			if (observingSystemComponentRef != null) {
				product.addObservingSystemComponentsItem(observingSystemComponentRef);
			}
			
		}
		
		for (String reference_role : ep.TARGET_ROLES) {
			Reference targetReference = ep.geReference(reference_role);
			if (targetReference != null) {
				product.addTargetsItem(targetReference);
			}
		}
		*/
		
		ArrayList<Reference> investigations = new ArrayList<Reference>();
		ArrayList<Reference> osc = new ArrayList<Reference>();
		ArrayList<Reference> targets = new ArrayList<Reference>();
		List<String> refIDs = ep.getReferenceLidVid();
		List<String> refTypes = ep.getReferenceRoles();
		Metadata meta = new Metadata();
		String baseURL = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

		String version = ep.getVersion();
		if (version != null) {
			meta.setVersion(ep.getVersion());
		}
		
		String creationDateTime = ep.getCreationDate();
		if (creationDateTime != null) {
			meta.setCreationDateTime(ep.getCreationDate());
		}
		
		/* TO DO reactivate it 
		String updateDateTime = ep.getModificationDate();
		if (updateDateTime != null) {
			meta.setUpdateDateTime(updateDateTime);
		}
		*/
		
		String labelUrl = ep.getPDS4FileRef();
		if (labelUrl != null) {		
			meta.setLabelUrl(labelUrl);
		}

		/*
		if (refIDs.size() == refTypes.size())
		{
			for (int i=0 ; i < refIDs.size(); i++)
			{
				switch (refTypes.get(i))
				{
					case "collection_to_investigation":
						ElasticSearchUtil.append(investigations, refIDs.get(i), baseURL);
						break;
					case "is_instrument":
						ElasticSearchUtil.append(osc, refIDs.get(i), baseURL);
						break;
					case "collection_to_target":
						ElasticSearchUtil.append(targets, refIDs.get(i), baseURL);
						break;
					default: break;
				}
			}
		}
		else
		{
			// PANIC: how could this ever happen!!!
			log.error("The number of reference IDs does not match the number of reference types.");
		}
		*/
		
		for (String id : ep.getRef_lid_instrument()) { ElasticSearchUtil.append (osc, id, baseURL); }
		ElasticSearchUtil.append (investigations, ep.getRef_lid_investigation(), baseURL);
		ElasticSearchUtil.append (targets, ep.getRef_lid_target(), baseURL);
		product.setLabelXml(ep.getPDS4XML()); // value is injected to be used as-is in XML serialization
		product.setInvestigations(investigations);
		product.setMetadata(meta);
		product.setObservingSystemComponents(osc);
		product.setTargets(targets);
		return product;
	
		
	}

}
