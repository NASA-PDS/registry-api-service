package gov.nasa.pds.api.engineering.controllers;

import java.util.List;


public class ProductsRequest
{
    public String q;
    public String keyword;
    
    public Integer start;
    public Integer limit;
    
    public List<String> fields;
    public List<String> sort;
    
    public Boolean onlySummary;
    
    
    public void setSearchInfo(String q, String keyword)
    {
        this.q = q;
        this.keyword = keyword;
    }
    
    
    public void setFieldInfo(List<String> fields, List<String> sort, Boolean onlySummary)
    {
        this.fields = fields;
        this.sort = sort;
        this.onlySummary = onlySummary;
    }
    
    
    public void setPageInfo(Integer start, Integer limit)
    {
        this.start = start;
        this.limit = limit;
    }
}
