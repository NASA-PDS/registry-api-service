package gov.nasa.pds.api.engineering.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nasa.pds.model.Pds4Product;


public class Pds4JsonProductSerializer extends AbstractHttpMessageConverter<Pds4Product>
{
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
        wr.write("\"pds4\": " + "{}\n");
        
        wr.write("}\n");
        wr.close();
    }

}
