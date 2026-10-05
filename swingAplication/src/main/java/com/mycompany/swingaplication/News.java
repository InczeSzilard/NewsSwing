
package com.mycompany.swingaplication;


public class News {
    private String title;
    private String link;
    private String date;
    
    public News (String title,String link,String date){
        this.title=title;
        this.link=link;
        this.date=date;
    }
    public String getTitle(){
        return title;
    }
    public String getLink(){
        return link;
    }
    public String getDate(){
        return date;
    }
    @Override
    public String toString(){
        return title + "\n" + link + "\n" + date;
    }
}
