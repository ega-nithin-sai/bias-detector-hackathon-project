// package com.biascheck.biasdetector.model;

// public class ArticleRequest {
//     private String title;
//     private String url;
//     private String text;

//     public String getTitle() {return title;}
//     public void setTitle (String title ){ this.title=title;}

//     public String getUrl (){return url; }
//     public void setUrl (String url){this.url =url;}

//     public String getText(){return text;}
//     public void setText (String text){this.text=text;}
    
//     }

package com.biascheck.biasdetector.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleRequest {
    private String url;
    private String text;
}
