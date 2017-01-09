package edu.nyu.cs.cs2580;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  private int totalTerms;

  public void setTotalTerms(int total){
    totalTerms = total;
  }

  public int getTotalTerms(){
    return totalTerms;
  }

  public DocumentIndexed(int docid) {
    super(docid);
  }
}
