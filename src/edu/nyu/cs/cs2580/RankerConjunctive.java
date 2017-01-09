package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Instructors' code for illustration purpose. Non-tested code.
 * 
 * @author congyu
 */
public class RankerConjunctive extends Ranker {

  public RankerConjunctive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    Document doc = null;
    int docid = -1;
    while ((doc = _indexer.nextDoc(query, docid)) != null) {
      rankQueue.add(scoreDocument(doc,query));
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
      docid = doc._docid;
    }

    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    ScoredDocument scoredDoc = null;
    while ((scoredDoc = rankQueue.poll()) != null) {
      results.add(scoredDoc);
    }
    Collections.sort(results, Collections.reverseOrder());
    return results;
  }

  /*
    Scores Document based on Query Likelyhood probability
 */
  private ScoredDocument scoreDocument(Document doc, Query query) {


    double queryLikelyhoodProbability = 1.0;
    double totalTermsInDoc = ((DocumentIndexed)doc).getTotalTerms();
    double totalTermsInCourpus = _indexer.totalTermFrequency();
    double lambda = 0.5;

    for(String queryToken : query._tokens){
      double termFrequency = _indexer.documentTermFrequency(queryToken,doc._docid);
      double corpusTermFrequency = _indexer.corpusDocFrequencyByTerm(queryToken);
      queryLikelyhoodProbability *= (1-lambda)*(termFrequency/totalTermsInDoc)+(lambda)*(corpusTermFrequency/totalTermsInCourpus);
    }

    if (query instanceof QueryPhrase) {
      for (Vector<String> phraseTokens : ((QueryPhrase) query)._phraseTokens) {
        for(String queryToken : phraseTokens){
          double termFrequency = _indexer.documentTermFrequency(queryToken,doc._docid);
          double corpusTermFrequency = _indexer.corpusDocFrequencyByTerm(queryToken);
          queryLikelyhoodProbability *= (1-lambda)*(termFrequency/totalTermsInDoc)+(lambda)*(corpusTermFrequency/totalTermsInCourpus);
        }
      }
    }
    float pageRank = doc.getPageRank();
    int numviews = doc.getNumViews();
    double docConjScore =0.0;
    docConjScore = queryLikelyhoodProbability * 0.65;
    docConjScore += 0.39*(Math.log(pageRank) / Math.log(2));
    docConjScore += 0.0001*(Math.log(numviews) / Math.log(2));
    docConjScore = Math.pow(2, docConjScore);

    return new ScoredDocument(doc, docConjScore);
  }
}
