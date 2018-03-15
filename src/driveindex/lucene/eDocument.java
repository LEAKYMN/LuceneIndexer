
package driveindex.lucene;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;

/**
 *
 * @author Philip M. Trenwith
 */
public class eDocument 
{  
  public static String TAG_ID = "ID";
  public static String TAG_Path = "Path";
  public static String TAG_Filename = "Filename";
  public static String TAG_Extension = "Extension";
  public static String TAG_Category = "Category";
  public static String TAG_Size = "Size";
  
  public String sFileAbsolutePath = "";
  public String sFilePath = "";
  public String sFileName = "";
  public String sFileExtension = "";
  public String sFileCategory = "";
  public long lFileSize = 0;
  
  public static eDocument from(Document oDocument)
  {
    eDocument oReturn = new eDocument();
    oReturn.sFileAbsolutePath = oDocument.get(TAG_ID);
    oReturn.sFilePath = oDocument.get(TAG_Path);
    oReturn.sFileName = oDocument.get(TAG_Filename);
    oReturn.sFileExtension = oDocument.get(TAG_Extension);
    oReturn.sFileCategory = oDocument.get(TAG_Category);
    oReturn.lFileSize = Long.parseLong(oDocument.get(TAG_Size));
    return oReturn;
  }
  
  public String getFormattedFileSize()
  {
    return FileUtils.byteCountToDisplaySize(lFileSize);
  }
}
