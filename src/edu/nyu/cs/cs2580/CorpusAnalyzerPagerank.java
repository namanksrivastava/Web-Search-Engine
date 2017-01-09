package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

  private final int _iterCount =2;
  private final float _lambda = 0.9f;
  private final String _outFilePath = _options._indexPrefix + "/PageRank.tsv";

  private Vector<Float> _ranks = new Vector<>();
  private Map<String, Integer> _docNameIDMap = new HashMap<String, Integer>();
  private Vector<HashSet<Integer>> _docLinks = new Vector<HashSet<Integer>>();
  private Vector<Integer> _docOutgoingLinkCount = new Vector<Integer>();
  private Vector<Integer> _docIncomingLinkCount = new Vector<Integer>();


  public CorpusAnalyzerPagerank(Options options) {
    super(options);
  }

  /**
   * This function processes the corpus as specified inside {@link _options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   * 
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   * 
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
   *
   * @throws IOException
   */
  @Override
  public void prepare() throws IOException {
    System.out.println("Preparing " + this.getClass().getName());
    processFiles(_options._corpusPrefix);
  }

  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   * 
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());

    // _V is the initial rank vector for which
    // every page's importance is uniformly
    // distributed
    Vector<Float> _V = new Vector<> ();
    int totalDocs = _docNameIDMap.size();

    for(int i = 0; i < totalDocs; i++) {
      _V.add(1.0f);
    }

    for(int i = 0; i< _iterCount; i++) {

      //Google matrix's component for dangling link
      Float val1 = 0.0f;
      for(int d=0;d<totalDocs;d++){
        val1 = ((1.0f - _lambda)/totalDocs)*_V.get(d);
      }
      _ranks = new Vector<>();

      for (int j = 0; j < totalDocs; j++) {
        _ranks.add(val1);
      }

      for (Map.Entry<String, Integer> entry : _docNameIDMap.entrySet()) {

        HashSet<Integer> outLinks = _docLinks.get(entry.getValue());
        int docId = _docNameIDMap.get(entry.getKey());

        //If there are outlinks miltiply and add value with corresponding _Vector
        /*if(outLinks.size() > 0) {
          for(int docId : outLinks) {
            System.out.println(docId);
            //int docId = outLinks.get(d);
            Float val = _ranks.get(docId);
            int linksCount = _docOutgoingLinkCount.get(docId);
            val += (_lambda)*(_V.get(docId)/linksCount);
            System.out.println(_V.get(entry.getValue()));
            _ranks.set(docId, val);
          }
        }
        */

        //GoogleMatrix Transpose Matrix Rank Calculation
        float outLinkRankVal = 0.0f;
        if (outLinks.size() > 0) {
          for (int currDocId = 0; currDocId < totalDocs; currDocId++) {
            if (_docLinks.get(currDocId).contains(docId)) {
              int linksCount = _docOutgoingLinkCount.get(currDocId);
              //int linksCount = _docLinks.get(currDocId).size();
              if(linksCount!=0){
                outLinkRankVal += (_lambda) * (1.0f / linksCount) * _V.get(currDocId);
              }
            }
          }
          Float val = _ranks.get(docId);
          val += outLinkRankVal;
          _ranks.set(docId, val);
        }
      }
      _V = _ranks;
    }
    savePageRanks();
  }

  public void savePageRanks() throws IOException{

    FileWriter fw = new FileWriter(_outFilePath);
    BufferedWriter bw = new BufferedWriter(fw);
    String docName;
    int index,docID;
    float pageRankScore;
    double score;
    for (Map.Entry<String, Integer> nameIDPair : _docNameIDMap.entrySet()){
      docName = (String)nameIDPair.getKey();
      docID = (Integer) nameIDPair.getValue();
      pageRankScore = _ranks.get(docID);
      bw.write(docName + "\t" +Double.toString(pageRankScore));
      bw.newLine();
    }
    bw.close();
  }


  public Map<String, Double> loadPageRanks() throws IOException{
    Map<String,Double> pageRankMap = new HashMap<>();
    FileReader fr = new FileReader(_outFilePath);
    BufferedReader br = new BufferedReader(fr);

    String line;
    while((line = br.readLine()) !=null){
      String[] tokens = line.split("\t");
      String docName = tokens[0];
      double rank = Double.parseDouble(tokens[1]);
      pageRankMap.put(docName,rank);
    }
    return  pageRankMap;
  }

  /**
   * During indexing mode, this function loads the PageRank values computed
   * during mining mode to be used by the indexer.
   *
   * @throws IOException
   */
  @Override
  public Map<String, Double> load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    return loadPageRanks();
  }

  private void processFiles(String dir) throws IOException {
    File[] fileNames = new File(dir).listFiles();
    int count=0;

    try {

      for (File file : fileNames) {

        if (file.isFile() && !file.isHidden()) {
          _docNameIDMap.put(file.getName(), count);
          _docOutgoingLinkCount.add(count, 0);
          _docIncomingLinkCount.add(count, 0);
          _docLinks.add(new HashSet<Integer>());
          //extractLinks(file);
          count++;
        } else if (file.isDirectory()) {
          //not recursively going inside a directory
          continue;
          //processFiles(dir+file.getName());
        }
      }
      for (File file : fileNames) {
        extractLinks(file);
      }
    } catch (Exception e) {

    }

    System.out.println("DocSize ::::::: "+_docLinks.size());
  }

  public void extractLinks(File file) throws IOException {

    int sourceDocId = _docNameIDMap.get(file.getName());
    HeuristicLinkExtractor hl = new HeuristicLinkExtractor(file);
    String next_link;
    while ((next_link = hl.getNextInCorpusLinkTarget())!=null) {

      //Only check for the links in the corpus
      if(_docNameIDMap.get(next_link)!=null){
        int destDocId = _docNameIDMap.get(next_link);
        //New Link(of a doc in the corpus - Increment the link count
        if (!_docLinks.get(sourceDocId).contains(destDocId)&&destDocId!=sourceDocId) {
          _docLinks.get(sourceDocId).add(destDocId);
          _docOutgoingLinkCount.set(sourceDocId, _docOutgoingLinkCount.get(sourceDocId) + 1);
          _docIncomingLinkCount.set(destDocId,_docIncomingLinkCount.get(destDocId)+1);
        }

      }
    }

  }

  public static void main(String[] args) throws IOException {
    CorpusAnalyzerPagerank corp = new CorpusAnalyzerPagerank(new Options("conf/engine.conf"));
    corp.prepare();
    corp.compute();

  }
}
