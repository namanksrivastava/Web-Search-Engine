package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {

  private Map<String, Double> numViews = new HashMap<>();

  public LogMinerNumviews(Options options) {
    super(options);
  }

  /**
   * This function processes the logs within the log directory as specified by
   * the {@link _options}. The logs are obtained from Wikipedia dumps and have
   * the following format per line: [language]<space>[article]<space>[#views].
   * Those view information are to be extracted for documents in our corpus and
   * stored somewhere to be used during indexing.
   *
   * Note that the log contains view information for all articles in Wikipedia
   * and it is necessary to locate the information about articles within our
   * corpus.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    retrieveDocuments();
    parseLogFile();
    writeToFile();
  }

  private void writeToFile() throws IOException {
    String outputPath =  _options._indexPrefix + "/numViews.tsv";

    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath));

    for (String docName : numViews.keySet()) {
      bufferedWriter.write(docName + "\t" + numViews.get(docName).toString());
      bufferedWriter.newLine();
    }
    bufferedWriter.close();
  }

  private void parseLogFile() throws IOException {

    BufferedReader bufferedReader;
    for (File file : new File(_options._logPrefix).listFiles()) {
      try {
        if (!file.isDirectory() && !file.isHidden()) {
          bufferedReader = new BufferedReader(new FileReader(file));
          String line;
          while ((line = bufferedReader.readLine()) != null) {
            String[] splits = line.split(" ");

            try {
              String docName = java.net.URLDecoder.decode(splits[1], "UTF-8");

              if (numViews.containsKey(docName)) {
                numViews.put(docName, numViews.get(docName) + Integer.parseInt(splits[2]));
              }
            } catch (Exception e) {

            }
          }
        }
      } catch (Exception e){

      }
    }
  }

  private void retrieveDocuments() {
    for (File file : new File(_options._corpusPrefix).listFiles()) {
      if (!file.isDirectory() && !file.isHidden()) {
        numViews.put(file.getName(), 0.0 );
      }
    }
  }

  /**
   * During indexing mode, this function loads the NumViews values computed
   * during mining mode to be used by the indexer.
   * 
   * @throws IOException
   */
  @Override
  public Map<String, Double> load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    BufferedReader bufferedReader = new BufferedReader(new FileReader(_options._indexPrefix + "/numViews.tsv"));
    String line;
    String splits[];
    while ((line = bufferedReader.readLine()) != null) {
      splits = line.split("\t");
      numViews.put(splits[0], Double.parseDouble(splits[1]));
    }
    return numViews;
  }

  public static void main(String args[]) throws IOException {
    LogMinerNumviews log = new LogMinerNumviews(new Options("conf/engine.conf"));
    log.compute();
    Map<String, Double> load = log.load();
    System.out.println();
  }
}
