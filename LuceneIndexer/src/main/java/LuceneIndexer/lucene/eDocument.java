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
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.lucene.document.Document;

/**
 *
 * @author Philip M. Trenwith
 */
public class eDocument 
{  
  public final static String TAG_ID = "ID";
  public final static String TAG_Path = "Path";
  public final static String TAG_Filename = "Filename";
  public final static String TAG_Extension = "Extension";
  public final static String TAG_Category = "Category";
  public final static String TAG_Size = "Size";
  public final static String TAG_Hash = "Hash";
  
  private StringProperty m_oFileAbsolutePath = new SimpleStringProperty();
  private StringProperty m_oFilePath = new SimpleStringProperty();
  private StringProperty m_oFileName = new SimpleStringProperty();
  private StringProperty m_oFileExtension = new SimpleStringProperty();
  private StringProperty m_oFileCategory = new SimpleStringProperty();
  private StringProperty m_oFileHash = new SimpleStringProperty();
  private LongProperty m_oFileSize = new SimpleLongProperty(0);
  
  public static eDocument from(Document oDocument)
  {
    eDocument oReturn = new eDocument();
    oReturn.m_oFileAbsolutePath = new SimpleStringProperty(oDocument.get(TAG_ID));
    oReturn.m_oFilePath = new SimpleStringProperty(oDocument.get(TAG_Path));
    oReturn.m_oFileName = new SimpleStringProperty(oDocument.get(TAG_Filename));
    oReturn.m_oFileExtension = new SimpleStringProperty(oDocument.get(TAG_Extension));
    oReturn.m_oFileCategory = new SimpleStringProperty(oDocument.get(TAG_Category));
    if (cConfig.instance().getHashDocuments())
    {
      oReturn.m_oFileHash = new SimpleStringProperty(oDocument.get(TAG_Hash));
    }
    oReturn.m_oFileSize = new SimpleLongProperty(Long.parseLong(oDocument.get(TAG_Size)));
    return oReturn;
  }
  
  public final StringProperty absolutePathProperty() 
  {
    return this.m_oFileAbsolutePath;
  }
  
  public final StringProperty pathProperty() 
  {
    return this.m_oFilePath;
  }
  
  public final StringProperty filenameProperty() 
  {
    return this.m_oFileName;
  }
  
  public final StringProperty extensionProperty() 
  {
    return this.m_oFileExtension;
  }
  
  public final StringProperty categoryProperty() 
  {
    return this.m_oFileCategory;
  }
  
  public final LongProperty sizeProperty() 
  {
    return this.m_oFileSize;
  }

  public final StringProperty hashProperty() 
  {
    return this.m_oFileHash;
  }

  public StringProperty getProperty(String sPropertyName)
  {
    StringProperty oReturn = new SimpleStringProperty("Invalid Property Name: " + sPropertyName);
    switch (sPropertyName)
    {
      case TAG_Path:
      {
        oReturn = m_oFilePath;
        break;
      }
      case TAG_Filename:
      {
        oReturn = m_oFileName;
        break;
      }
      case TAG_Extension:
      {
        oReturn = m_oFileExtension;
        break;
      }
      case TAG_Category:
      {
        oReturn = m_oFileCategory;
        break;
      }
      case TAG_Hash:
      {
        oReturn = m_oFileHash;
        break;
      }
    }
    
    return oReturn;
  }
}
