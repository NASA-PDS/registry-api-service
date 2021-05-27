package gov.nasa.pds.api.engineering.serializer;

import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.util.ClassUtils;

import gov.nasa.pds.api.model.ProductWithXmlLabel;
import gov.nasa.pds.model.Product;

public class XmlProductSerializer extends Jaxb2RootElementHttpMessageConverter {
	
	public XmlProductSerializer() {
	      super();
	}

	@Override
	protected boolean supports(Class<?> clazz) {
	      return ProductWithXmlLabel.class.isAssignableFrom(clazz);
	 }
	
	 
	@Override
	protected void writeToResult(Object o, HttpHeaders headers, Result result) throws Exception {
		
		o = ((ProductWithXmlLabel)o).labelXml(null);
		
		super.writeToResult(o, headers, result);
	}

}
