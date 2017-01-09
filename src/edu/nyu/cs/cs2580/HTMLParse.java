package edu.nyu.cs.cs2580;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.File;
import java.io.IOException;

/**
 * Created by sanchitmehta on 26/10/16.
 */

public class HTMLParse {

    private String _bodyText;
    private String _title;


    public HTMLDocument getDocument(File fileName) throws IOException {

        org.jsoup.nodes.Document doc = Jsoup.parse(fileName, "UTF-8", "https://en.wikipedia.org/wiki/Courant_Institute_of_Mathematical_Sciences");
        Element body = doc.body();
        _bodyText = new String(body.text().toLowerCase());
        _title = doc.title().replaceFirst("- Wikipedia, the free encyclopedia","");
        _bodyText = TextProcessor.regexRemoval(_bodyText);

        HTMLDocument _htmlDocument = new HTMLDocument(_bodyText.toString(),_title.toString(),fileName.getAbsolutePath());
        return _htmlDocument;
    }


}
