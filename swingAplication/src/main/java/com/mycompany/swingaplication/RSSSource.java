package com.mycompany.swingaplication;

public class RSSSource {

    private String name;
    private String url;
  private String language;
  
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return name;
    }
   
public String getLanguage() {
return language; 
}
}