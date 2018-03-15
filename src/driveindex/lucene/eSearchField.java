
package driveindex.lucene;

/**
 *
 * @author Philip M. Trenwith
 */
public class eSearchField 
{
  private String sField;
  private String sValue; 
  
  public eSearchField(String sField, String sValue)
  {
    this.sField = sField;
    this.sValue = sValue;
  }
  
  public String getField()
  {
    return sField;
  }
  
  public String getValue()
  {
    return sValue;
  }
}
