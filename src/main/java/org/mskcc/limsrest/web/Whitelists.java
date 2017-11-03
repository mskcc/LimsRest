package org.mskcc.limsrest.web;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
  Just being extra stringent about what we let through as parameters, mostly for parameters that will be used by the data record manager to do sql queries. I'm hopeful the lims is resistant to attack vectors this way, but I haven't tested
*/
public class Whitelists{
    Pattern requestPattern ;
    Pattern sampleNamePattern;
    Pattern containsALetter;
    Pattern genericParamPattern;
    Pattern picklistPattern;
    Pattern filePathPattern;
    Pattern servicePattern;
    Pattern specialParamPattern;

    public Whitelists(){
        requestPattern = Pattern.compile("^[0-9]{5,}[A-Z_]*$");
        sampleNamePattern = Pattern.compile("^[A-Za-z0-9_-]+$");
        containsALetter = Pattern.compile(".*[a-zA-Z].*");
        picklistPattern = Pattern.compile("^[A-Za-z0-9 ()/-]+$");
        genericParamPattern = Pattern.compile("^[ A-Za-z0-9_'.+-]+$");
        filePathPattern = Pattern.compile("^[A-Za-z0-9_/.-]*$");
        servicePattern = Pattern.compile("^(IGO|igo|iGO|iGo)[-_][0-9]{4,8}$");
        specialParamPattern = Pattern.compile("^[ A-Za-z0-9_'().+-]+$");
    }
   public String serviceFormatText(){
       return "The service id must be igo  followed by a dash or hyphen followed by numbers"; 
    }
   public boolean serviceMatches(String service){
        if(service == null || service.equals("NULL"))
            return true;
        return servicePattern.matcher(service).matches(); //banked requests can have "igo/IGO prefix
   }
   

   public String requestFormatText(){
        return "The format must be five digits followed an optional underscore and capital letter";
   }

   public boolean requestMatches(String request){
        if(request == null || request.equals("NULL"))
            return true;
        return requestPattern.matcher(request).matches();
   }

   public String filePathFormatText(){
       return "The format must be a valid unix file path";
   }

   public boolean filePathMatches(String path){
       if(path == null || path.equals("NULL"))
           return true;
       return filePathPattern.matcher(path).matches();
   }

   public String sampleFormatText(){
        return "The format must only contain upper case and lower case letters, numbers, underscores and dashes";

   }

   public boolean sampleMatches(String sample){
        if(sample == null || sample.equals("NULL"))
            return true;
        return sampleNamePattern.matcher(sample).matches() ;
   }

   public String picklistFormatText(){
        return "The format must only contain upper case and lower case letters, numbers, spaces, parenthesis, slashes and dashes";
   }

   public boolean picklistMatches(String list){
        if(list == null || list.equals("NULL"))
            return true;
        return picklistPattern.matcher(list).matches();
   }


   public String textFormatText(){
        return "The format must only contain upper case and lower case letters, numbers, underscores, single quotes, periods, plusses and minuses";
   }

   public boolean textMatches(String text){
        if(text == null || text.equals("NULL"))
            return true;
        return genericParamPattern.matcher(text).matches();
   }

   public String specialFormatText(){
        return "The format must only contain upper case and lower case letters, numbers, underscores, single quotes, periods, plusses and minuses and params";
   }

   public boolean specialMatches(String text){
        if(text == null || text.equals("NULL"))
            return true;
        return specialParamPattern.matcher(text).matches();
   }

}
