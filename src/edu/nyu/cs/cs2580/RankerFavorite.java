package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 * Ranker (except RankerPhrase) from HW1. The new Ranker should no longer rely
 * on the instructors' {@link IndexerFullScan}, instead it should use one of
 * your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

  public RankerFavorite(Options options,
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

    return new ScoredDocument(doc, queryLikelyhoodProbability);
  }
}
