package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by naman on 11/11/2016.
 */
public class Bhattacharyya {
    private static Options _options = null;

    public Bhattacharyya(Options options) {
        _options = options;
    }

    public static void main(String[] args) throws IOException {
        Bhattacharyya bc = new Bhattacharyya(new Options("conf/engine.conf"));
        String pathPrfOutput, pathOutputFile;

        pathOutputFile = args[1];

        if(args[0].contains(".tsv")){
            String tsvFileName = args[0];
            File tsvFile = new File(tsvFileName);
            if(!tsvFile.exists()){
                throw (new IOException( tsvFileName + " file not found!!"));
            }
            bc.querySimilarity(tsvFile, pathOutputFile);
        } else {
            pathPrfOutput = args[0];
            bc.querySimilarity(pathPrfOutput, pathOutputFile);
        }
    }

    private void querySimilarity(File tsvFile, String pathOutputFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(tsvFile));
        FileWriter outputFile = new FileWriter(pathOutputFile);
        BufferedWriter output = new BufferedWriter(outputFile);
        ArrayList<String> queriesList = new ArrayList<>();
        HashMap<String, String > queryToFileName = new HashMap<>();
        String fileLine;

        while ((fileLine = reader.readLine()) != null){
            String sNext[] = fileLine.split(":");
            String query = sNext[0];
            queriesList.add(query);
            queryToFileName.put(query, sNext[1]);
        }

        for(int i=0; i < queriesList.size(); i++){
            double beta = 0.0;
            String fileName = queryToFileName.get(queriesList.get(i));
            File file1 = new File(fileName);
            if(!file1.exists()){
                break;
            }

            BufferedReader reader1 = new BufferedReader(new FileReader(file1));
            String line;
            HashMap<String, Double> queryProbabilityMap = new HashMap<String, Double>();
            while ((line = reader1.readLine()) != null) {
                Scanner s = new Scanner(line);
                queryProbabilityMap.put(s.next(), Double.parseDouble(s.next()));
            }
            reader1.close();

            for (int j=0; j < queriesList.size(); j++){
                if(i==j) continue;
                String fileName2 = queryToFileName.get(queriesList.get(j));
                File file2 = new File(fileName2);
                if (!file2.exists()) {
                    break;
                }
                BufferedReader reader2 = new BufferedReader(new FileReader(file2));
                String queryTerm;
                double probability = 0.0;
                while ((line = reader2.readLine()) != null) {
                    Scanner s = new Scanner(line);
                    queryTerm = s.next();
                    probability = Double.parseDouble(s.next());
                    if (queryProbabilityMap.containsKey(queryTerm)) {
                        beta += Math.sqrt(queryProbabilityMap.get(queryTerm) * probability);
                    }
                }
                reader2.close();
                output.write(queriesList.get(i) + "\t" + queriesList.get(j) + "\t" + Double.toString(beta) + "\n");
            }
        }
        output.close();
    }

    public void querySimilarity(String pathPrfOutput, String pathOutputFile) throws IOException {
        File queryFolder = new File(pathPrfOutput);
        File[] fileList = queryFolder.listFiles();
        FileWriter outputFile = new FileWriter(pathOutputFile);
        BufferedWriter output = new BufferedWriter(outputFile);
        ArrayList<String> queriesList = getAllQueries();

        for (int i = 1; i <= fileList.length; i++) {
            double beta = 0.0;

            File file1 = new File(pathPrfOutput + "/prf-" + i + ".tsv");
            if(!file1.exists()){
                break;
            }

            BufferedReader reader1 = new BufferedReader(new FileReader(file1));
            String line;
            HashMap<String, Double> queryProbabilityMap = new HashMap<String, Double>();
            while ((line = reader1.readLine()) != null) {
                Scanner s = new Scanner(line);
                queryProbabilityMap.put(s.next().toString(), Double.parseDouble(s.next()));
            }
            reader1.close();

            for (int j = 1; j <= fileList.length; j++) {

                if(i==j) continue;
                File file2 = new File(pathPrfOutput + "/prf-" + j + ".tsv");
                if (!file2.exists()) {
                    break;
                }

                BufferedReader reader2 = new BufferedReader(new FileReader(file2));
                String queryTerm = null;
                double probability = 0.0;
                while ((line = reader2.readLine()) != null) {
                    Scanner s = new Scanner(line);
                    queryTerm = s.next().toString();
                    probability = Double.parseDouble(s.next());
                    if (queryProbabilityMap.containsKey(queryTerm)) {
                        beta += Math.sqrt(queryProbabilityMap.get(queryTerm) * probability);
                    }
                }
                reader2.close();
                output.write(queriesList.get(i) + "\t" + queriesList.get(j) + "\t" + Double.toString(beta) + "\n");
            }
        }
        output.close();
    }

    private ArrayList<String> getAllQueries() throws IOException {
        BufferedReader br =  new BufferedReader(new FileReader(_options._dataPrefix +"/queries.tsv"));
        String line;
        ArrayList<String> queryList = new ArrayList<String>();
        while((line = br.readLine()) != null){
            queryList.add(line);
        }
        br.close();
        return queryList;
    }
}
