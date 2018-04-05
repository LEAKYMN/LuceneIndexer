/*
 * Copyright (C) 2018 Philip M. Trenwith
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package LuceneIndexer.lucene;

import LuceneIndexer.cConfig;
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
  public static String TAG_Hash = "Hash";
  
  public String sFileAbsolutePath = "";
  public String sFilePath = "";
  public String sFileName = "";
  public String sFileExtension = "";
  public String sFileCategory = "";
  public String sFileHash = "";
  public long lFileSize = 0;
  
  public static eDocument from(Document oDocument)
  {
    eDocument oReturn = new eDocument();
    oReturn.sFileAbsolutePath = oDocument.get(TAG_ID);
    oReturn.sFilePath = oDocument.get(TAG_Path);
    oReturn.sFileName = oDocument.get(TAG_Filename);
    oReturn.sFileExtension = oDocument.get(TAG_Extension);
    oReturn.sFileCategory = oDocument.get(TAG_Category);
    if (cConfig.instance().getHashDocuments())
    {
      oReturn.sFileHash = oDocument.get(TAG_Hash);
    }
    oReturn.lFileSize = Long.parseLong(oDocument.get(TAG_Size));
    return oReturn;
  }
  
  public String getFormattedFileSize()
  {
    return FileUtils.byteCountToDisplaySize(lFileSize);
  }
}
