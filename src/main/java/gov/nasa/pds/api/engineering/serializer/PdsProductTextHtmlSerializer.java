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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nasa.pds.model.PdsProduct;

public class PdsProductTextHtmlSerializer extends AbstractHttpMessageConverter<PdsProduct>
{
	public PdsProductTextHtmlSerializer()
	{
        super(new MediaType("text","html"));
	}

	@Override
	protected boolean supports(Class<?> clazz) { return PdsProduct.class.isAssignableFrom(clazz); }

	@Override
	protected PdsProduct readInternal(Class<? extends PdsProduct> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException { return new PdsProduct(); }

	@Override
	protected void writeInternal(PdsProduct t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException
	{
		MappingJackson2HttpMessageConverter asJson = new MappingJackson2HttpMessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
	    asJson.setObjectMapper(mapper);
	    asJson.setPrettyPrint(true);

        OutputStream os = outputMessage.getBody();
        OutputStreamWriter wr = new OutputStreamWriter(os);
        asJson.write(t, MediaType.TEXT_HTML, outputMessage);
        wr.close();
	}
}
