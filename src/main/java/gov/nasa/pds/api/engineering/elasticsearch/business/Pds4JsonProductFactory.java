package gov.nasa.pds.api.engineering.elasticsearch.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gov.nasa.pds.model.Pds4Metadata;
import gov.nasa.pds.model.Pds4MetadataDataFiles;
import gov.nasa.pds.model.Pds4Product;


public class Pds4JsonProductFactory
{
    private static final String FLD_JSON_BLOB = "ops:Label_File_Info/ops:json_blob";
    private static final String FLD_FILE_NAME = "ops:Data_File_Info/ops:file_name";
    private static final String FLD_CREATION_DATE = "ops:Data_File_Info/ops:creation_date_time";
    private static final String FLD_FILE_REF = "ops:Data_File_Info/ops:file_ref";
    private static final String FLD_FILE_SIZE = "ops:Data_File_Info/ops:file_size";
    private static final String FLD_MD5 = "ops:Data_File_Info/ops:md5_checksum";
    private static final String FLD_MIME_TYPE = "ops:Data_File_Info/ops:mime_type";
    
    
    public static Pds4Product createProduct(String lidvid, Map<String, Object> fieldMap)
    {
        Pds4Product prod = new Pds4Product();
        prod.setId(lidvid);
        
        // JSON BLOB
        Object obj = fieldMap.get(FLD_JSON_BLOB);
        if(obj != null)
        {
            prod.setPds4(obj);
        }
        
        // File Metadata
        prod.setMetadata(createMetadata(fieldMap));

        return prod;
    }
    

    @SuppressWarnings("rawtypes")
    private static Pds4Metadata createMetadata(Map<String, Object> fieldMap)
    {
        Object obj = fieldMap.get(FLD_FILE_NAME);
        if(obj == null) return null;

        Pds4Metadata meta = new Pds4Metadata();
        
        if(obj instanceof List)
        {
            List nameList = (List)obj;
            List dateList = (List)fieldMap.get(FLD_CREATION_DATE);
            List refList = (List)fieldMap.get(FLD_FILE_REF);
            List sizeList = (List)fieldMap.get(FLD_FILE_SIZE);
            List md5List = (List)fieldMap.get(FLD_MD5);
            List mimeList = (List)fieldMap.get(FLD_MIME_TYPE);            
            
            List<Pds4MetadataDataFiles> items = new ArrayList<>(nameList.size());
            meta.setDataFiles(items);
            
            for(int i = 0; i < nameList.size(); i++)
            {
                Pds4MetadataDataFiles item = new Pds4MetadataDataFiles();
                items.add(item);
                item.setFileName((String)nameList.get(i));
                item.setCreationDate((String)dateList.get(i));
                item.setFileRef((String)refList.get(i));
                item.setFileSize((String)sizeList.get(i));
                item.setMd5Checksum((String)md5List.get(i));
                item.setMimeType((String)mimeList.get(i));
            }
        }
        else
        {
            String val = (String)obj;
            List<Pds4MetadataDataFiles> items = new ArrayList<>(1);
            meta.setDataFiles(items);
            
            Pds4MetadataDataFiles item = new Pds4MetadataDataFiles();
            items.add(item);
            item.setFileName(val);
            
            val = (String)fieldMap.get(FLD_CREATION_DATE);
            item.setCreationDate(val);
            
            val = (String)fieldMap.get(FLD_FILE_REF);
            item.setFileRef(val);
            
            val = (String)fieldMap.get(FLD_FILE_SIZE);
            item.setFileSize(val);

            val = (String)fieldMap.get(FLD_MD5);
            item.setMd5Checksum(val);

            val = (String)fieldMap.get(FLD_MIME_TYPE);
            item.setMimeType(val);
        }

        return meta;
    }
}
