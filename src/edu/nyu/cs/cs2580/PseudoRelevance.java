package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by naman on 11/10/2016.
 */
public class PseudoRelevance {
    private Options _options = null;
    private Indexer _indexer = null;

    //for each term in al documents bein considered, contains the sum of frequency of the term in all those documents
    private HashMap<String, Integer> _term = new HashMap<>();

    //For each docId as key contains a list of terms and the frequency of those terms as value
    //private HashMap<Integer, HashMap<String,Integer>> _postings = new HashMap<>();



    public PseudoRelevance(Options options, Indexer indexer) {
        _options = options;
        _indexer = indexer;
    }

    public Vector<TermObject> queryRepresentation(Vector<ScoredDocument> Results, int numTerms) throws IOException {
        for (ScoredDocument scoredDoc : Results) {
            Document doc = scoredDoc._doc;
            loadDataToPostingListForDoc(doc);
        }
        return printProbability(numTerms);
    }

    public Vector<TermObject> printProbability(int numTems){
        //Contains a term object which has term and the terms probability, could be sorted using the terms probability
        Vector <TermObject> termProbability = new Vector<TermObject>();
        int  totalTermFrequency = totalTermFrequency();
        for(String term : _term.keySet()) {
            double probability = (double)_term.get(term) / (double)(totalTermFrequency - _term.get(term));
            TermObject tobj = new TermObject(term, probability);
            termProbability.add(tobj);
        }
        Collections.sort(termProbability, Collections.reverseOrder());
        double totalProbability = 0.0;
        for (int i = 0; i < termProbability.size() && i < numTems; ++i) {
            totalProbability += termProbability.get(i)._termProbability;
        }

        Vector<TermObject> response = new Vector<>();
        for (int i = 0; i < termProbability.size() && i < numTems; ++i) {
            TermObject termObject = termProbability.get(i);
            termObject._termProbability = (termObject._termProbability)/totalProbability;
            response.add(new TermObject(termObject._term, termObject._termProbability));
        }


        return response;
    }

    private int totalTermFrequency(){
        int total = 0;
        for(String term : _term.keySet()){
            total += _term.get(term);
        }
        return total;
    }

    public void loadDataToPostingListForDoc(Document doc) throws IOException {
        String fileName = _options._indexPrefix + "/Documents/" + doc._docid;
        File docFile = new File(fileName);
        if(docFile.exists()) {
            byte[] bytes = Files.readAllBytes(new File(fileName).toPath());
            Vector<Byte> vb = new Vector<>();
            for (byte b : bytes) {
                vb.add(b);
            }

            Vector<Integer> numbers = IndexCompressor.vByteDecoder(vb);
            int i = 0;
            int docId = numbers.get(i++);
            while (i < numbers.size()) {
                int num = numbers.get(i);
                String mapKey = ((IndexerInvertedCompressed)_indexer)._terms.get(num);
                if (_term.containsKey(mapKey)) {
                    _term.put(mapKey, _term.get(mapKey) + numbers.get(i + 1));
                } else {
                    _term.put(mapKey, numbers.get(i + 1));
                }
                i += 2;
            }
        }
    }

    class TermObject implements Comparable<TermObject>{
        public String _term;
        public double _termProbability;

        public TermObject(String term, double termProbability){
            _term = term;
            _termProbability = termProbability;
        }

        @Override
        public int compareTo(TermObject o) {
            if (this._termProbability == o._termProbability) {
                return 0;
            }
            return (this._termProbability > o._termProbability) ? 1 : -1;
        }
    }
}
