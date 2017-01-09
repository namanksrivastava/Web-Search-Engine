package edu.nyu.cs.cs2580;

/**
 * Created by naman on 10/29/2016.
 */
public class TextProcessor {
    private static final String[] removalRegex= {"((http://)|(https://))[^\\s]*[\\s]",
            "(\\[[0-9]*\\])",
            "(\\[edit\\])" ,
            "\\p{Punct}",
            "[\\s][^\\s][\\s]"};

    private static final String[] stopWords = {"the","or","and","be","of","as","from","that","for","to","is","was","it","has","had","etc","shall","a","but","him","his","if","an","in"};

    public static String regexRemoval(String _text){
        for(String regex : removalRegex){
            _text = _text.replaceAll(regex," ");
        }

        for(String stopWord : stopWords){
            _text = _text.replaceAll("[\\s]" + stopWord + "[\\s]", " ");
        }
        return _text;
    }
}
