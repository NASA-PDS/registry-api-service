package gov.nasa.pds.api.engineering.elasticsearch.entities;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.Inflater;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EntitytProductWithBlob extends EntityProduct {
	
	@JsonProperty("ops:Label_File_Info/ops:blob")
	private String fileBlob;
	
	public String getPDS4XML() {
		
		if (this.fileBlob == null) {
    		return "no xml available";
    	}
		
    	Inflater iflr = new Inflater();
    	ByteArrayOutputStream baos = null;
    	
    	byte[] decodedCompressedBytes = Base64.getDecoder().decode(this.fileBlob);
    	
    	iflr.setInput(decodedCompressedBytes);
    	baos = new ByteArrayOutputStream();
        byte[] tmp = new byte[4*1024];
      
        try{
        	
       
            while(!iflr.finished()){
                int size = iflr.inflate(tmp);
                baos.write(tmp, 0, size);
            }
            
            return baos.toString("utf-8");
            
        } catch (Exception ex){
             
        } finally {
            try{
                if(baos != null) baos.close();
            } catch(Exception ex){}
        }
        
        return "no xml available";
    	
    }



}
