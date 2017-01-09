package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public Vector<Vector<String>> _phraseTokens = new Vector<>();

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
    if(_query == null){
      return;
    }

    _query = _query.toLowerCase();
    Scanner s = new Scanner(_query);

    Pattern pattern = Pattern.compile("\"[^\"]*\"");
    String phrase;
    while ((phrase = s.findInLine(pattern)) != null) {
      _query = _query.replace(phrase, "");
      phrase = TextProcessor.regexRemoval(phrase);

      Vector<String> token = new Vector<>();

      Scanner s1 = new Scanner(phrase);
      Stemmer stemmer = new Stemmer();
      while (s1.hasNext()) {
        String term = s1.next();
        stemmer.add(term.toCharArray(), term.length());
        stemmer.stem();
        token.add(stemmer.toString());
      }
      s1.close();
      _phraseTokens.add(token);
    }
    s.close();

    _query = TextProcessor.regexRemoval(_query);
    Scanner s1 = new Scanner(_query);
    Stemmer stemmer = new Stemmer();
    while (s1.hasNext()) {
      String term = s1.next();
      stemmer.add(term.toCharArray(), term.length());
      stemmer.stem();
      _tokens.add(stemmer.toString());
    }
    s1.close();
  }
}
