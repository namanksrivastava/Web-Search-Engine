package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3 based on your {@code RankerFavorite}
 * from HW2. The new Ranker should now combine both term features and the
 * document-level features including the PageRank and the NumViews. 
 */
public class RankerComprehensive extends Ranker {

  public RankerComprehensive(Options options,
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
    double docCompScore =0.0;
    float maxPageRank = 1.0f;
    float maxNumViews = 1;
    try {
      String maxVal = _options._corpusPrefix + "maxVals.txt";
      BufferedReader reader = new BufferedReader(new FileReader(maxVal));
      maxNumViews = Integer.parseInt(reader.readLine());
      maxPageRank = Float.parseFloat(reader.readLine());
    }catch (Exception e){

    }

    docCompScore = queryLikelyhoodProbability * 0.85;
    docCompScore += 0.000022*(pageRank/maxPageRank);
    docCompScore += 0.0000001*(numviews/maxNumViews);
    //docConjScore = Math.pow(2, docConjScore);

    return new ScoredDocument(doc, docCompScore);
  }
}
