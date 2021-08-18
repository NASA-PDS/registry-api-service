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

import gov.nasa.pds.api.engineering.elasticsearch.BlobUtil;
import gov.nasa.pds.model.Pds4Product;

/**
 * Custom serializer to write a Pds4Product in "pds4+json" format.
 * @author karpenko
 */
public class Pds4JsonProductSerializer extends AbstractHttpMessageConverter<Pds4Product>
{
    private static final Logger log = LoggerFactory.getLogger(Pds4JsonProductSerializer.class);
            
    /**
     * Constructor
     */
    public Pds4JsonProductSerializer()
    {
        super(new MediaType("application", "pds4+json"));
    }

    
    @Override
    protected boolean supports(Class<?> clazz)
    {
        return Pds4Product.class.isAssignableFrom(clazz);
    }

    
    @Override
    protected Pds4Product readInternal(Class<? extends Pds4Product> clazz, HttpInputMessage msg)
            throws IOException, HttpMessageNotReadableException
    {
        return new Pds4Product();
    }

    
    @Override
    public void writeInternal(Pds4Product product, HttpOutputMessage msg)
            throws IOException, HttpMessageNotWritableException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        
        OutputStream os = msg.getBody();
        OutputStreamWriter wr = new OutputStreamWriter(os);
        
        wr.write("{\n");

        String value = objectMapper.writeValueAsString(product.getId());
        wr.write("\"id\": " + value + ",\n");
        
        value = objectMapper.writeValueAsString(product.getMetadata());
        wr.write("\"meta\": " + value + ",\n");
        
        Object obj = product.getPds4();
        if(obj != null)
        {
            try
            {
                String pds4json = BlobUtil.blobToString((String)obj);
                wr.write("\"pds4\": ");
                wr.write(pds4json);
            }
            catch(Exception ex)
            {
                log.warn("Could not extract BLOB from product " + product.getId(), ex);
            }
        }
        
        wr.write("}\n");
        wr.close();
    }

}
