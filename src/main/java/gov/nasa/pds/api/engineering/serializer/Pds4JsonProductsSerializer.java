package gov.nasa.pds.api.engineering.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nasa.pds.model.Pds4Products;

/**
 * Custom serializer to write a Pds4Product in "pds4+json" format.
 * @author karpenko
 */
public class Pds4JsonProductsSerializer extends AbstractHttpMessageConverter<Pds4Products>
{
    private static final Logger log = LoggerFactory.getLogger(Pds4JsonProductsSerializer.class);
            
    /**
     * Constructor
     */
    public Pds4JsonProductsSerializer()
    {
        super(new MediaType("application", "pds4+json"));
    }

    
    @Override
    protected boolean supports(Class<?> clazz)
    {
        return Pds4Products.class.isAssignableFrom(clazz);
    }

    
    @Override
    protected Pds4Products readInternal(Class<? extends Pds4Products> clazz, HttpInputMessage msg)
            throws IOException, HttpMessageNotReadableException
    {
        return new Pds4Products();
    }

    
    @Override
    public void writeInternal(Pds4Products products, HttpOutputMessage msg)
            throws IOException, HttpMessageNotWritableException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        
        OutputStream os = msg.getBody();
        OutputStreamWriter wr = new OutputStreamWriter(os);
        
        wr.write("{\n");
        
        // Summary
        wr.write("\"summary\":");
        String value = mapper.writeValueAsString(products.getSummary());
        wr.write(value);
        wr.write(",\n");
        
        // Data
        wr.write("\"data\":[");
        
        wr.write("]\n");
                
        wr.write("}\n");
        wr.close();
    }

}
