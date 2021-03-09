package gov.nasa.pds.api.engineering.elasticsearch;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
		
		Metadata meta = new Metadata();
		
		
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
		
		product.setLabelXml(ep.getPDS4XML()); // value is injected to be used as-is in XML serialization
		
		product.setMetadata(meta);
			
		try
		{
			ArrayList<Reference> investigations = new ArrayList<Reference>();
			Document xmldoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse (new InputSource(new StringReader(ep.getPDS4XML())));
			NodeList investigation_areas = xmldoc.getElementsByTagName("Investigation_Area");

			for (int index=0 ; index < investigation_areas.getLength() ; index++)
			{
				investigations.add(new Reference());
				investigations.get(index).setTitle(((Element)investigation_areas.item(index)).getElementsByTagName("name").item(0).getTextContent());
				investigations.get(index).setRef(((Element)investigation_areas.item(index)).getElementsByTagName("lid_reference").item(0).getTextContent());
				investigations.get(index).setType(((Element)investigation_areas.item(index)).getElementsByTagName("type").item(0).getTextContent());
				investigations.get(index).setDescription("");
			}
			product.setInvestigations(investigations);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return product;
	
		
	}

}
