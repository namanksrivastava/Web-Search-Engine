package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Created by naman on 11/9/2016.
 */
public class Spearman {
    private static Options _options = null;
    private Vector <DocData> _pageRankData = new Vector<DocData>();
    private Vector <DocData> _numViewsData = new Vector<DocData>();


    public Spearman(Options options) {
        _options = options;
    }

    public static void main (String[] args) throws IOException, ClassNotFoundException {
        Spearman sp = new Spearman(new Options("conf/engine.conf"));
        if(args.length > 0) {
            sp.loadData(args[0], args[1]);
        } else {
            sp.loadData("", "");
        }
        System.out.println(sp.computeSpearman());
    }

    public double computeSpearman(){

        Map <String, Integer> pageRank = rank(_pageRankData);
        Map <String, Integer> numViews = rank(_numViewsData);
        double x_bar = (pageRank.size()+1)/2.0;//calculateXBar();
        double y_bar = (numViews.size()+1)/2.0;//calculateXBar();

        double numerator = 0.0;
        double denominatorSummationPageRank = 0.0;
        double denominatorSummationNumViews = 0.0;
        for (String docName : pageRank.keySet()){
            numerator += (pageRank.get(docName) - x_bar)*(numViews.get(docName) - y_bar);
            denominatorSummationPageRank += Math.pow(pageRank.get(docName) - x_bar, 2);
            denominatorSummationNumViews += Math.pow(numViews.get(docName) - y_bar, 2);
        }
        double denominator = Math.sqrt(denominatorSummationNumViews * denominatorSummationPageRank);
        if (denominator == 0.0) {
            return 0.0;
        }
        return numerator/denominator;
    }

    public void loadData(String pageRank, String numviews) throws IOException, ClassNotFoundException {
        if(pageRank.length() <= 0) {
            CorpusAnalyzerPagerank corpusAnalyzerPagerank = new CorpusAnalyzerPagerank(new Options("conf/engine.conf"));
            Map<String, Double> _pageRank = corpusAnalyzerPagerank.load();
            _pageRankData = MapToObjectVector(_pageRank);
        } else {
            File pageRankInputFile = new File(pageRank);
            if(!pageRankInputFile.exists()) {
                throw (new IOException("Page rank file not found!!"));
            }
            FileReader fr = new FileReader(pageRank);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while((line = br.readLine()) !=null){
                String[] tokens = line.split("\t");
                String docName = tokens[0];
                double rank = Double.parseDouble(tokens[1]);
                DocData docObject = new DocData(docName, rank);
                _pageRankData.add(docObject);
            }
        }

        if(numviews.length() <= 0) {
            LogMinerNumviews logMinerNumviews = new LogMinerNumviews(new Options("conf/engine.conf"));
            Map<String, Double> _numViews = logMinerNumviews.load();
            _numViewsData = MapToObjectVector(_numViews);
        } else {
            File numViewsInputFile = new File(numviews);
            if(!numViewsInputFile.exists()) {
                throw (new IOException("Num views file not found!!"));
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(numviews));
            String line;
            String splits[];
            while ((line = bufferedReader.readLine()) != null) {
                splits = line.split("\t");
                DocData docObject = new DocData(splits[0],Double.parseDouble(splits[1]));
                _numViewsData.add(docObject);
            }
        }
    }

    private Vector<DocData> MapToObjectVector(Map<String, Double> rankMap) {
        Vector<DocData> rankVector = new Vector<>();
        for (String docName : rankMap.keySet()){
            DocData docDataObject = new DocData(docName, rankMap.get(docName));
            rankVector.add(docDataObject);
        }
        return rankVector;
    }

    public HashMap <String, Integer> rank(Vector<DocData> docForRanking){
        HashMap <String, Integer> rankedDoc = new HashMap<>();
        Collections.sort(docForRanking, Collections.reverseOrder());
        int i=0;
        for(DocData doc: docForRanking){
            i++;
            rankedDoc.put(doc._docName, i);
        }
        return rankedDoc;
    }

    class DocData implements Comparable <DocData> {
        public String  _docName;
        public double _data;

        public DocData (String docName, double data){
            _docName = docName;
            _data = data;
        }

        public int compareTo(DocData o) {
            if (this._data == o._data) {
                return this._docName.compareTo(o._docName);
            }
            return (this._data > o._data) ? 1 : -1;
        }
    }

    /*   public HashMap <String, Integer> rank(Vector<DocData> docForRanking){
        HashMap <String, Integer> rankedDoc = new HashMap<>();
        Collections.sort(docForRanking, Collections.reverseOrder());
        int i=0;
        double prevDoc_data = -1.0;
        for(DocData doc: docForRanking){
            if(doc._data != prevDoc_data){
                i++;
                prevDoc_data = doc._data;
            }
            rankedDoc.put(doc._docName, i);
        }
        return rankedDoc;
    }

    public double calculateXBar(){
        int total = 0;
        for (Integer key : _pageRank.keySet()){
            total += _pageRank.get(key);
        }
        return ((double) total)/((double)_pageRank.size());
    }

    public double calculateYBar(){
        int total = 0;
        for (Integer key : _numViews.keySet()){
            total += _numViews.get(key);
        }
        return ((double) total)/((double)-_numViews.size());
    }*/
}
