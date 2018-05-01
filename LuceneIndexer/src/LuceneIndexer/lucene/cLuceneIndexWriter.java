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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author Philip M. Trenwith
 */
public class cLuceneIndexWriter extends Observable
{
  private final Object m_oLock = new Object();
  private IndexWriter m_oIndexWriter = null;
  private cIndex m_oIndex;
  
  public cLuceneIndexWriter(cIndex oIndex)
  {
    m_oIndex = oIndex;
  }
  
  public void open()
  {
    //addObserver(cDriveIndexUI.instance());
    FSDirectory m_oIndexDirectory;
    try 
    {
      Path oPath = new File(m_oIndex.getIndexLocation()).toPath();
      m_oIndexDirectory = FSDirectory.open(oPath, NoLockFactory.INSTANCE);
      Analyzer oDefaultAnalyzer = new StandardAnalyzer(CharArraySet.copy(Collections.emptySet()));

      //PerFieldAnalyzerWrapper oMultipleLanguageAnalyzer = new PerFieldAnalyzerWrapper(oDefaultAnalyzer, m_oLanguageFields);
      IndexWriterConfig oIndexConfig = new IndexWriterConfig(oDefaultAnalyzer);

      m_oIndexWriter = new IndexWriter(m_oIndexDirectory, oIndexConfig);
    }
    catch (Exception ex) 
    {
      ex.printStackTrace();
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public boolean indexFile(File oFile, String sFileHash)
  {
    boolean bResult = false;
    try
    {
      synchronized (m_oLock)
      {
        Document oDocument = new Document();

        // TO Stroe fields with positioning information
        //FieldType fieldType = new FieldType(TextField.TYPE_STORED);
  //      fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS );
  //      fieldType.setStoreTermVectors(true);
  //      fieldType.setStoreTermVectorPositions(true);
  //      fieldType.setStoreTermVectorPayloads(true);
  //      fieldType.setStoreTermVectorOffsets(true);
  //      fieldType.setTokenized( true );
  //      oDocument.add(new Field(sField, sValue, fieldType));

        //oDocument.add(new Field(eDocument.TAG_ID, oFile.getAbsolutePath(), new FieldType(TextField.TYPE_STORED)));
        oDocument.add(new StringField(eDocument.TAG_ID, oFile.getPath(), Field.Store.YES));
        
        oDocument.add(new Field(eDocument.TAG_Path, oFile.getParentFile().getAbsolutePath(), new FieldType(TextField.TYPE_STORED)));
        oDocument.add(new Field(eDocument.TAG_Filename, FilenameUtils.getBaseName(oFile.getName()), new FieldType(TextField.TYPE_STORED)));
        oDocument.add(new Field(eDocument.TAG_Extension, FilenameUtils.getExtension(oFile.getAbsolutePath()).toLowerCase(), new FieldType(TextField.TYPE_STORED)));
        oDocument.add(new Field(eDocument.TAG_Category, cConfig.instance().getCategory(oFile), new FieldType(TextField.TYPE_STORED)));
        oDocument.add(new Field(eDocument.TAG_Size, oFile.length()+"", new FieldType(TextField.TYPE_STORED)));

        if (cConfig.instance().getHashDocuments())
        {
          oDocument.add(new Field(eDocument.TAG_Hash, sFileHash, new FieldType(TextField.TYPE_STORED)));
        }

        m_oIndexWriter.updateDocument(new Term(eDocument.TAG_ID, oFile.getAbsolutePath()), oDocument);
        bResult = true;
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(cLuceneIndexWriter.class.getName()).log(Level.SEVERE, null, ex);
    }
    return bResult;
  }
  
  public boolean deleteFile(File oFile)
  {
    boolean bResult = false;
    if (oFile != null)
    {
      try
      {
        synchronized (m_oLock)
        {
          Term term = new Term(eDocument.TAG_ID, new BytesRef(oFile.getAbsolutePath()));

          System.out.println("Deleting file from index: " + term.toString());
          m_oIndexWriter.deleteDocuments(term);

          bResult = true;
        }
      }
      catch (Exception ex)
      {
        bResult = false;
        Logger.getLogger(cLuceneIndexWriter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return bResult;
  }
  
  public void close()
  {
    synchronized (m_oLock)
    {
      if (m_oIndexWriter != null)
      {
        try
        {
          m_oIndexWriter.commit();
          m_oIndexWriter.close();
        }
        catch (IOException ex)
        {
          Logger.getLogger(cLuceneIndexWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally 
        {
          m_oIndexWriter = null;
        }
      }
    }
  }
  
  public void commit()
  {
    synchronized (m_oLock)
    {
      if (m_oIndexWriter != null)
      {
        try
        {
          if (m_oIndexWriter.isOpen())
          {
            m_oIndexWriter.commit();
          }
        }
        catch (IOException ex)
        {
          Logger.getLogger(cLuceneIndexWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }
  
  public void setStatus(String sStatus)
  {
    setChanged();
    notifyObservers(sStatus);
  }
}
