package com.mycompany.swingaplication;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;

public class RSSLoader {
    public RSSparse loadSources(){
    Gson gson=new Gson();
    try{
    FileReader reader=new FileReader("src/main/resources/rssSources.json");
    RSSparse sources=gson.fromJson(reader,RSSparse.class);
    
    reader.close();
    
    return sources;
    } catch(IOException e){
    e.printStackTrace();
    return null;
    }
    }
}
