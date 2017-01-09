package edu.nyu.cs.cs2580;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {

  private static int FILE_COUNT_FOR_INDEX_SPLIT = 500;

  private static int TERM_COUNT_FOR_INDEX_SPLIT = 500;

  private int indexCount = 0;
  private int partCount = 0;

  public int maxNumViews = 0;
  public float maxPageRank = 0.0f;

  // Maps each term to their integer representation
  private Map<String, Integer> _dictionary = new HashMap<String, Integer>();
  // All unique terms appeared in corpus. Offsets are integer representations.
  public Vector<String> _terms = new Vector<String>();

  // Stores all Document in memory.
  private Vector<Document> _documents = new Vector<Document>();

  private Map<Integer, Vector<Integer>> _postings = new HashMap<>();

  //Stores the index of each docid in posting list in sorted order(acc to doc ids)
  private Map<Integer,Vector<Integer>> _skipList = new HashMap<>();

  public IndexerInvertedCompressed() {
  }

  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    String corpusDir = _options._corpusPrefix;
    String indexDir  = _options._indexPrefix;

    deleteExistingFile(indexDir);

    processFiles(corpusDir);

    System.out.println("Created partial indexes. Now merging them");

    mergeIndex();

    System.out.println("Splitting index file based on number of terms");

    splitIndexFile();

    System.out.println(
            "Indexed " + Integer.toString(_numDocs) + " docs with " +
                    Long.toString(_terms.size()) + " terms.");

    _postings = null;

    writeIndexerObjectToFile();

    compressMergedFiles(indexDir);

  }

  private List<String> stringTokenizer(String str) {
    List<String> tokenList = new ArrayList<String>();
    try {
      StringTokenizer st = new StringTokenizer(str, "\t");
      while (st.hasMoreElements()) {
        tokenList.add(st.nextElement().toString());
      }
    }catch (Exception e) {
      //IT is null
    }
    return tokenList;
  }

  public void compressDocTermFile(String dir, int docid, Vector<Integer> termIds) throws IOException {

    OutputStream os = new FileOutputStream(dir + "/" + docid, true);

    Vector<Byte> bytes1 = IndexCompressor.vByteEncoder(docid);

    byte[] bArr1 = new byte[bytes1.size()];

    for (int i = 0 ; i < bArr1.length; i++) {
      bArr1[i] = bytes1.get(i);
    }

    os.write(bArr1);

    Vector<Integer> terms = new Vector<>();

    int i = 0;
    while (i < termIds.size()){
      int offset = termIds.get(i);

      terms.add(offset);
      terms.add(termIds.get(i+1));

      i += 2;
    }

    Vector<Byte> bytes3 = IndexCompressor.vByteEncoder(terms);

    byte[] bArr3 = new byte[bytes3.size()];

    for (int j = 0 ; j < bArr3.length; j++) {
      bArr3[j] = bytes3.get(j);
    }

    os.write(bArr3);

    os.close();
  }

  public void compressMergedFiles(String dir) throws IOException {

    for (int part = 0; part <= partCount; part++) {
      File file = new File(dir + "/index-part-" + part + ".tsv");
      if (file.isFile() && !file.isHidden() && !file.getName().toLowerCase().contains("object.idx")) {
        OutputStream os = new FileOutputStream(dir + "/index-comp-part-"+ part +".tsv", true);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
          String line;
          while ((line = br.readLine()) != null) {

            Vector<Byte> bytes1 = IndexCompressor.vByteEncoder(Integer.parseInt(line));

            byte[] bArr1 = new byte[bytes1.size()];

            for (int i = 0 ; i < bArr1.length; i++) {
              bArr1[i] = bytes1.get(i);
            }

            os.write(bArr1);

            line = br.readLine();
            List<String> strings = stringTokenizer(line);

            Vector<Byte> bytes2 = IndexCompressor.vByteEncoder(strings.size());

            byte[] bArr2 = new byte[bytes2.size()];

            for (int i = 0 ; i < bArr2.length; i++) {
              bArr2[i] = bytes2.get(i);
            }

            os.write(bArr2);

            Vector<Integer> post = new Vector<>();

            int docIdPos = 0;
            int lastDocId = 0;
            int lastOcc = 0;
            int num;
            for (int i = 0; i < strings.size(); i++) {
              String word = strings.get(i);
              int scn = Integer.parseInt(word);

              if (i == docIdPos) {
                num = scn - lastDocId;
                lastDocId = scn;
                lastOcc = 0;
              }
              else if (i == docIdPos + 1) {
                num = scn;
                docIdPos += scn + 2;
              }
              else {
                num = scn - lastOcc;
                lastOcc = scn;
              }

              post.add(num);
            }

            Vector<Byte> bytes3 = IndexCompressor.vByteEncoder(post);

            byte[] bArr3 = new byte[bytes3.size()];

            for (int i = 0 ; i < bArr3.length; i++) {
              bArr3[i] = bytes3.get(i);
            }

            os.write(bArr3);
          }
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }

        os.close();
      } else if (file.isDirectory()) {
        //not recursively going inside a directory
        continue;
        //processFiles(dir+file.getName());
      }
      file.delete();
    }

  }

  private void writeIndexerObjectToFile() throws IOException {
    String indexFile = _options._indexPrefix + "/objects.idx";
    System.out.println("Store other objects to: " + indexFile);
    ObjectOutputStream writer =
            new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();
  }

  private void deleteExistingFile(String indexDir) {
    for(File file: new File(indexDir).listFiles()) {
      if(!file.getName().toLowerCase().contains("numviews.tsv")&&!file.getName().toLowerCase().contains("pagerank.tsv")){
        if (file.isDirectory()) {
          for (File subFile : file.listFiles()) {
            subFile.delete();
          }
        }
        file.delete();
      }
    }
  }

  private void processFiles(String dir) throws IOException {
    System.out.println("Inside Directory : "+dir);
    File[] fileNames = new File(dir).listFiles();
    System.out.println("Construct index from: " + dir);
    HTMLParse htmlParse = new HTMLParse();

    String docsDir = _options._indexPrefix + "/Documents";
    File directory = new File(docsDir);

    if (!directory.exists()) {
      directory.mkdir();
    }

    Map<String, Double> numViews = (Map<String, Double>) _logMiner.load();
    Map<String, Double> pageRanks = (Map<String, Double>) _corpusAnalyzer.load();
    int fileNum = 0;

    for (File file : fileNames) {

      try {

        if (file.isFile() && !file.isHidden()) {
          HTMLDocument htmlDocument = htmlParse.getDocument(file);
          DocumentIndexed doc = new DocumentIndexed(_documents.size());

          processDocument(htmlDocument.getBodyText(), doc);

          doc.setTitle(htmlDocument.getTitle());
          doc.setUrl(htmlDocument.getUrl());
          int currNumViews = numViews.get(file.getName()).intValue();
          maxNumViews = currNumViews > maxNumViews ? currNumViews : maxNumViews;
          doc.setNumViews(currNumViews);
          float currPageRank = pageRanks.get(file.getName()).floatValue();
          maxPageRank = currPageRank > maxPageRank ? currPageRank : maxPageRank;
          doc.setPageRank(currPageRank);
          _documents.add(doc);
          ++_numDocs;

          if (fileNum == FILE_COUNT_FOR_INDEX_SPLIT) {
            indexCount++;
            System.out.println("Constructing partial index number: " + indexCount);

            persistToFile(indexCount);
            fileNum = 0;
          }

          fileNum++;
        } else if (file.isDirectory()) {
          //not recursively going inside a directory
          continue;
          //processFiles(dir+file.getName());
        }
      } catch (Exception e) {

      }
    }

    String maxVal = _options._corpusPrefix + "maxVals.txt";
    BufferedWriter writer = new BufferedWriter(new FileWriter(maxVal, true));
    writer.write(maxNumViews + "\n" + maxPageRank);
    writer.close();

    indexCount++;
    System.out.println("Constructing partial index number: " + indexCount);
    persistToFile(indexCount);
  }

  private void splitIndexFile() throws IOException {
    String indexFile = _options._indexPrefix + "/corpus.tsv";
    BufferedReader reader = new BufferedReader(new FileReader(indexFile));

    String partFile = _options._indexPrefix + "/index-part-0.tsv";
    BufferedWriter writer = new BufferedWriter(new FileWriter(partFile, true));

    int count = -1;
    String line;
    while((line = reader.readLine()) != null) {
      count++;

      if (count == TERM_COUNT_FOR_INDEX_SPLIT) {
        count = 0;
        partCount++;
        writer.close();
        partFile = _options._indexPrefix + "/index-part-" + partCount + ".tsv";
        writer = new BufferedWriter(new FileWriter(partFile, true));
        writer.flush();
      }

      writer.write(line + "\n");
      line = reader.readLine();
      writer.write(line + "\n");


    }

    reader.close();
    writer.close();
    File index = new File(indexFile);
    index.delete();
  }

  private void mergeIndex() throws IOException {
    String indexFile = _options._indexPrefix + "/corpus.tsv";
    String firstFile = _options._indexPrefix + "/tempIndex1.tsv";

    File index = new File(indexFile);
    File first = new File(firstFile);

    if (indexCount == 1) {
      first.renameTo(index);
    }

    for (int i = 2; i <= indexCount; i++) {
      String secondFile = _options._indexPrefix + "/tempIndex" + i + ".tsv";

      File second = new File(secondFile);

      File mergedFile = mergeFiles(first, second);

      first.delete();
      second.delete();
      mergedFile.renameTo(index);
      first = new File(indexFile);
    }

  }

  private File mergeFiles(File first, File second) throws IOException {
    String tempFile = _options._indexPrefix + "/temp.tsv";

    File temp = new File(tempFile);
    BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));

    BufferedReader firstReader = new BufferedReader(new FileReader(first));
    BufferedReader secondReader = new BufferedReader(new FileReader(second));

    String lineInFirstFile = firstReader.readLine();
    String lineInSecondFile = secondReader.readLine();

    while ((lineInFirstFile != null) && (lineInSecondFile != null)) {

      if (Integer.parseInt(lineInFirstFile) < Integer.parseInt(lineInSecondFile)) {
        writer.write(lineInFirstFile + "\n");
        lineInFirstFile = firstReader.readLine();
        writer.write(lineInFirstFile + "\n");

        lineInFirstFile = firstReader.readLine();
      }
      else if (Integer.parseInt(lineInSecondFile) > Integer.parseInt(lineInFirstFile)) {
        writer.write(lineInSecondFile + "\n");
        lineInSecondFile = secondReader.readLine();
        writer.write(lineInSecondFile + "\n");

        lineInSecondFile = secondReader.readLine();
      }
      else {
        writer.write(lineInFirstFile + "\n");
        lineInFirstFile = firstReader.readLine();
        lineInSecondFile = secondReader.readLine();
        writer.write(lineInFirstFile + "\t" + lineInSecondFile + "\n");

        lineInFirstFile = firstReader.readLine();
        lineInSecondFile = secondReader.readLine();
      }

    }

    while (lineInFirstFile != null) {
      writer.write(lineInFirstFile + "\n");
      lineInFirstFile = firstReader.readLine();
    }

    while (lineInSecondFile != null) {
      writer.write(lineInSecondFile + "\n");
      lineInSecondFile = secondReader.readLine();
    }

    firstReader.close();
    secondReader.close();
    writer.close();

    return temp;
  }

  private void persistToFile(int index) throws IOException {
    String indexFile = _options._indexPrefix + "/tempIndex" + index + ".tsv";
    BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));

    List<Integer> termIds = new ArrayList<>();
    termIds.addAll(_postings.keySet());
    Collections.sort(termIds);

    for (Integer termId: termIds) {
      writer.write(termId.toString() + "\n");

      Vector<Integer> docOccs = _postings.get(termId);
      for (int i = 0; i < docOccs.size(); i++) {

        writer.write(docOccs.get(i).toString() + "\t");
      }
      writer.write("\n");
    }

    writer.close();
    _postings.clear();
  }

  private void processDocument(String content, DocumentIndexed doc) throws IOException {
    Scanner s = new Scanner(content);

    Map<String, Vector<Integer>> termOccurenceMap = new HashMap<>();

    int offset = 0;
    Stemmer stemmer = new Stemmer();
    while (s.hasNext()) {
      String term = s.next();
      stemmer.add(term.toCharArray(), term.length());
      stemmer.stem();
      term = stemmer.toString();

      if (!termOccurenceMap.containsKey(term)) {
        Vector<Integer> occurence = new Vector<>();
        occurence.add(doc._docid);
        occurence.add(1);
        occurence.add(offset);
        termOccurenceMap.put(term, occurence);
      }
      else {
        Vector<Integer> occurence = termOccurenceMap.get(term);
        occurence.set(1, occurence.get(1) + 1);
        occurence.add(offset);
      }
      offset++;
    }

    doc.setTotalTerms(offset);

    Vector<Integer> termIds = new Vector<>();

    for (String token : termOccurenceMap.keySet()) {
      int idx;
      if (_dictionary.containsKey(token)) {
        idx = _dictionary.get(token);
      } else {
        idx = _terms.size();
        _terms.add(token);
        _dictionary.put(token, idx);
      }

      termIds.add(idx);
      termIds.add(termOccurenceMap.get(token).get(1));

      if (_postings.containsKey(idx)) {
        _postings.get(idx).addAll(termOccurenceMap.get(token));
      }
      else {
        _postings.put(idx, termOccurenceMap.get(token));
      }

    }

    String docsDir = _options._indexPrefix + "/Documents";

    compressDocTermFile(docsDir, doc._docid, termIds);

    s.close();
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/objects.idx";
    System.out.println("Loading index objects other than postings list from: " + indexFile);

    ObjectInputStream reader =
            new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader.readObject();

    this._dictionary = loaded._dictionary;
    this._terms = loaded._terms;
    this._documents = loaded._documents;
    this.partCount = loaded.partCount;

    // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
    this._numDocs = _documents.size();
    for (Document doc : _documents) {
      this._totalTermFrequency += ((DocumentIndexed) doc).getTotalTerms();
    }

    reader.close();
  }

  @Override
  public Document getDoc(int docid) {
    return _documents.get(docid);
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public Document nextDoc(Query query, int docid) {

    if(query instanceof QueryPhrase){
      return nextDocPhrase((QueryPhrase) query, docid);
    } else {
      return nextDocIndividualTokens(query._tokens, docid);
    }
  }

  public Document nextDocIndividualTokens(Vector<String> queryTokens, int docid) {
    while (true) {
      List<Integer> idArray = new ArrayList<>();
      int maxId = -1;
      int sameDocId = -1;
      boolean allQueryTermsInSameDoc = true;
      for(String term : queryTokens){
        if (!_dictionary.containsKey(term)) {
          return null;
        }
        loadTermIfNotLoaded(term);
        idArray.add(next(term,docid));
      }
      for(int id : idArray){
        if(id == -1){
          return null;
        }
        if(sameDocId == -1){
          sameDocId = id;
        }
        if(id != sameDocId){
          allQueryTermsInSameDoc = false;
        }
        if(id > maxId){
          maxId = id;
        }
      }
      if(allQueryTermsInSameDoc){
        return _documents.get(sameDocId);
      }
      docid=maxId-1;
    }
  }

  public Document nextDocPhrase(QueryPhrase query, int docid){
    List<Integer> idArray = new ArrayList<>();
    int maxId = -1;
    int sameDocId = -1;
    boolean allQueryTermsInSameDoc = true;
    for(String term : query._tokens){
      if (!_dictionary.containsKey(term)) {
        return null;
      }
      loadTermIfNotLoaded(term);
      idArray.add(next(term,docid));
    }

    for (Vector<String> phraseTerms : query._phraseTokens) {
      idArray.add(nextForPhrase(phraseTerms, docid));
    }

    for(int id : idArray){
      if(id == -1){
        return null;
      }
      if(sameDocId == -1){
        sameDocId = id;
      }
      if(id != sameDocId){
        allQueryTermsInSameDoc = false;
      }
      if(id > maxId){
        maxId = id;
      }
    }
    if(allQueryTermsInSameDoc){
      return _documents.get(sameDocId);
    }
    return nextDocPhrase(query, maxId-1);
  }

  private int nextForPhrase(Vector<String> phraseTerms, int docid) {
    Document docForPhrase = nextDocIndividualTokens(phraseTerms, docid);
    if (docForPhrase == null) {
      return -1;
    }

    Map<String, Vector<Integer>> termPositionMap = getTermPositionMapForDoc(phraseTerms, docForPhrase._docid);

    String firstTerm = phraseTerms.get(0);
    for (int firstPos : termPositionMap.get(firstTerm)) {
      int i;
      for (i = 1; i < phraseTerms.size(); i++) {
        if (!termPositionMap.get(phraseTerms.get(i)).contains(firstPos + i)) {
          break;
        }
      }
      if (i == phraseTerms.size()) {
        return docForPhrase._docid;
      }
    }

    return nextForPhrase(phraseTerms, docForPhrase._docid);
  }

  private Map<String, Vector<Integer>> getTermPositionMapForDoc(Vector<String> phraseTerms, int docForPhrase) {
    Map<String, Vector<Integer>> termPosMap = new HashMap<>();

    Vector<Integer> posList = new Vector<>();
    for (String term : phraseTerms) {
      int docPos = binarySearchResultIndex(term, docForPhrase - 1);
      Vector<Integer> postingListforTerm = getPostingListforTerm(term);
      for (int i = 0 ; i < postingListforTerm.get(docPos + 1) ; i++) {
        posList.add(postingListforTerm.get(docPos + 2 + i));
      }
      termPosMap.put(term, posList);
    }

    return termPosMap;
  }

  public int next(String queryTerm, int docid){
    int binarySearchResultIndex = binarySearchResultIndex(queryTerm, docid);
    if (binarySearchResultIndex == -1)
      return -1;

    return getPostingListforTerm(queryTerm).get(binarySearchResultIndex);
  }

  private Vector<Integer> getPostingListforTerm(String term){
    return _postings.get(_dictionary.get(term));
  }

  private Vector<Integer> getSkipListforTerm(String term){
    return _skipList.get(_dictionary.get(term));
  }

  private int binarySearchResultIndex(String term, int current){
    Vector <Integer> PostingList = getPostingListforTerm(term);
    Vector <Integer> SkipList = getSkipListforTerm(term);
    int lt = SkipList.size()-1;
    if(lt == 0 || PostingList.get(SkipList.get(lt)) <= current){
      return -1;
    }
    if(PostingList.get(0)>current){
      return 0;
    }
    return binarySearch(PostingList,SkipList,0,lt,current);
  }

  private int binarySearch(Vector<Integer> PostingList, Vector<Integer> SkipList, int low, int high, int current){
    int mid;
    while(high - low > 1) {
      mid = (low + high) / 2;
      if (PostingList.get(SkipList.get(mid)) <= current) {
        low = mid;
      } else {
        high = mid;
      }
    }
    return SkipList.get(high);
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    loadTermIfNotLoaded(term);
    Vector<Integer> PostingList = getPostingListforTerm(term);
    int corpusDocFrequencyByTerm = 0;
    for(int i=0; i< PostingList.size()-1;){
      corpusDocFrequencyByTerm++;
      i += PostingList.get(i+1) + 2;
    }
    return corpusDocFrequencyByTerm;
  }

  @Override
  public int corpusTermFrequency(String term) {
    Vector<Integer> PostingList = getPostingListforTerm(term);
    int corpusTermFrequency = 0;
    for(int i=0; i< PostingList.size()-1;){
      corpusTermFrequency += PostingList.get(i+1);
      i += PostingList.get(i+1) + 2;
    }
    return corpusTermFrequency;
  }

  /**
   * @CS2580: Implement this to work with your RankerFavorite.
   */
  @Override
  public int documentTermFrequency(String term, int docid) {
    Vector<Integer> PostingList = getPostingListforTerm(term);
    for(int i=0; i< PostingList.size()-1;){
      if(docid == PostingList.get(i)){
        return  PostingList.get(i+1);
      } else {
        i += PostingList.get(i+1) + 2;
      }
    }
    return 0;
  }

  private void loadTermIfNotLoaded(String term) {
    if (!_postings.containsKey(_dictionary.get(term))) {
      try {
        loadIndexOnFlyForTerm(term);
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  private void loadIndexOnFlyForTerm(String term) throws IOException, ClassNotFoundException {
    _postings.clear();
    _skipList.clear();
    int termId = _dictionary.get(term);
    loadMiniIndex(termId/TERM_COUNT_FOR_INDEX_SPLIT);
  }

  private void loadMiniIndex(int indexNo) throws IOException {

    File idxFolder = new File(_options._indexPrefix);
    File[] indexFiles = idxFolder.listFiles();

    if (indexNo < indexFiles.length) {
      String fileName = _options._indexPrefix + "/index-comp-part-" + indexNo + ".tsv";

      byte[] bytes = Files.readAllBytes(new File(fileName).toPath());


      Vector<Byte> vb = new Vector<>();
      for (byte b : bytes) {
        vb.add(b);
      }

      Vector<Integer> numbers = IndexCompressor.vByteDecoder(vb);


      int i = 0;

      while (i < numbers.size()) {
        int termId = numbers.get(i);

        int postingSize = numbers.get(i+1);

        Vector<Integer> termPostingList = new Vector<>();

        int docIdPos = i+2;
        int lastDocId = 0;
        int lastOcc = 0;
        Integer num;
        int j;
        for (j = i+2; j < i+2+postingSize; j++) {

          if (j == docIdPos) {
            num = numbers.get(j) + lastDocId;
            lastDocId = num;
            lastOcc = 0;
          }
          else if (j == docIdPos + 1) {
            num = numbers.get(j);
            docIdPos += numbers.get(j) + 2;
          }
          else {
            num = numbers.get(j) + lastOcc;
            lastOcc = num;
          }
          termPostingList.add(num);
        }

        _postings.put(termId, termPostingList);

        Vector<Integer> skipPtrs = new Vector<>();
        int k = 0;
        while (k < termPostingList.size()) {
          skipPtrs.add(k);
          k += termPostingList.get(k + 1) + 2;
        }

        _skipList.put(termId, skipPtrs);

        i=j;
      }
    }
  }
}
