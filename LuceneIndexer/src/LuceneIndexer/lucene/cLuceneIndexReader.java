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
import LuceneIndexer.ui.fx.cMainLayoutController;
import LuceneIndexer.injection.cInjector;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;

/**
 *
 * @author Philip M. Trenwith
 */
public class cLuceneIndexReader extends Observable
{
  private long lDuplicateSearch = 0;
  private long lMaxDocs = 0;
  private DecimalFormat m_oDecimalFormat = new DecimalFormat("#.######");
  private IndexReader m_oIndexReader = null;
  private boolean m_bIsOpen = false;
  private cIndex m_oIndex;
  private Thread tProgressThread;
  private boolean m_bKeepAlive = true;
  private volatile boolean m_bDuplicationSearch = false; 
  private final Object oDuplicateLock = new Object();
  public cLuceneIndexReader(cIndex oIndex)
  {
    m_oIndex = oIndex;
    addObserver(cInjector.getInjector().getInstance(cMainLayoutController.class));
    
    tProgressThread = new Thread(()-> 
    {
      while (m_bKeepAlive)
      {
        if (!m_bDuplicationSearch)
        {
          synchronized (oDuplicateLock)
          {
            try
            {
              System.out.println("Entering wait state");
              oDuplicateLock.wait();
            }
            catch (InterruptedException ex)
            { }
          }
        }
        
        // m_oDecimalFormat.format(lDuplicateSearch/(lMaxDocs/100*100f))
        System.out.println("lDuplicateSearch: " + lDuplicateSearch + " (lMaxDocs) " + lMaxDocs);
        String sStatus = "Looking for duplicates: " + " " + lDuplicateSearch/(lMaxDocs/100*100f)*100  + " %";
        setStatus(sStatus);
        
        try
        {
          Thread.sleep(500);
        }
        catch (InterruptedException ex)
        { }
      }
    });
    tProgressThread.setName("ProgressThread");
    tProgressThread.start();
  }

  public boolean isOpen()
  {
    return m_bIsOpen;
  }

  public boolean open()
  {
    try
    {
      String sLocation = cConfig.instance().getIndexLocation();
      File oFile = new File(sLocation);
      if (!oFile.exists())
      {
        oFile.mkdirs();
      }

      Path oPath = new File(m_oIndex.getIndexLocation()).toPath();
      m_oIndexReader = DirectoryReader.open(FSDirectory.open(oPath, NoLockFactory.INSTANCE));
      return true;
    }
    catch (Exception ex)
    {
      if (ex instanceof IndexNotFoundException)
      {
        Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
        return false;
      }
      else
      {
        Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return false;
  }

  public int getNumberOfDocuments()
  {
    int iReturn = -1;

    if (m_oIndexReader != null)
    {
      iReturn = m_oIndexReader.maxDoc();
    }
    return iReturn;
  }

  public ArrayList<eDocument> getTopNDocuments(int n)
  {
    ArrayList<eDocument> lsReturn = new ArrayList();
    if (m_oIndexReader != null)
    {
      int iCeiling = Math.min(m_oIndexReader.maxDoc(), n);
      for (int i = 0; i < iCeiling; i++)
      {
        try
        {
          Document oDocument = m_oIndexReader.document(i);
          eDocument eDoc = eDocument.from(oDocument);
          lsReturn.add(eDoc);
        }
        catch (IOException ex)
        {
          Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    return lsReturn;
  }
  
  public HashMap<String, ArrayList<eDocument>> getDuplicateDocuments()
  {
    m_bDuplicationSearch = true;
    synchronized (oDuplicateLock)
    {
      oDuplicateLock.notify();
    }
    HashMap<String, ArrayList<eDocument>> oReturn = new HashMap();
    if (m_oIndexReader != null)
    {
      lDuplicateSearch = 0;
      lMaxDocs = m_oIndexReader.maxDoc();
      for (int i = 0; i < lMaxDocs; i++)
      {
        if (!m_bKeepAlive)
        {
          break;
        }
        try
        {
          Document oDocument = m_oIndexReader.document(i);
          eDocument eDoc = eDocument.from(oDocument);
          if (!oReturn.containsKey(eDoc.sFileHash) && eDoc.sFileHash != null )
          {
            eSearchField oSearchField = new eSearchField(eDocument.TAG_Hash, eDoc.sFileHash);
            ArrayList<eSearchField> lsSearchFields = new ArrayList<>(Arrays.asList(oSearchField));

            ArrayList<eDocument> oDocs = search(lsSearchFields, true, false, true);
            if (oDocs.size() > 1)
            {
              ArrayList<eDocument> lsReturn = oDocs;
              oReturn.put(eDoc.sFileHash, lsReturn);
            }
          }
        }
        catch (IOException ex)
        {
          Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    m_bDuplicationSearch = false;
    return oReturn;
  }

  public ArrayList<eDocument> search(ArrayList<eSearchField> lsSearchFields, boolean bWholeWords, boolean bCaseSensitive, boolean bLookingForDuplicates)
  {
    ArrayList<eDocument> lsResults = new ArrayList();
    
    if (m_oIndexReader != null)
    {
      try
      {
        Query oQuery = null;
        int iFieldLength = lsSearchFields.size();
        IndexSearcher oSearcher = new IndexSearcher(m_oIndexReader);
        //TermQuery query = new TermQuery(new Term(sField, sText));
        if (bWholeWords)
        {
          BooleanQuery.Builder builder1 = new BooleanQuery.Builder();
          for (int i = 0; i < iFieldLength; i++)
          {
            eSearchField oSearchField = lsSearchFields.get(i);
            String[] lsSplit = oSearchField.getValue().split(" ");
            PhraseQuery.Builder builder = new PhraseQuery.Builder();
            for (int j = 0; j < lsSplit.length; j++)
            {
              builder.add(new Term(oSearchField.getField(), lsSplit[j]), j);
            }
            oQuery = builder.build();

            builder1.add(oQuery, BooleanClause.Occur.MUST);
          }
          oQuery = builder1.build();

//          TopScoreDocCollector collector = TopScoreDocCollector.create(100);
//          String sLocation = cConfig.instance().getIndexLocation();
//          File oFile = new File(sLocation);
//Directory directory = new SimpleFSDirectory(oFile.toPath());
//IndexSearcher searcher = new IndexSearcher(m_oIndexReader);
//
//          Analyzer oDefaultAnalyzer = new StandardAnalyzer(CharArraySet.copy(Collections.emptySet()));
//          QueryParser oQueryParser = new QueryParser("filename", oDefaultAnalyzer);
//          oQueryParser.setDefaultOperator(QueryParser.Operator.AND);
//          oQuery = oQueryParser.parse("commons-io-2.4");
//          
//          searcher.search(oQuery, collector);
//          ScoreDoc[] hits = collector.topDocs().scoreDocs;
//          System.out.println(hits.length);
        }
        else
        {
          String sQuery = "";
          MultiFieldQueryParser oMultiFieldQueryParser = null;
          String[] lsFields = new String[iFieldLength];
          for (int i = 0; i < iFieldLength; i++)
          {
            eSearchField oSearchField = lsSearchFields.get(i);
            lsFields[i] = oSearchField.getField();
            if (i == iFieldLength - 1)
            {
              sQuery += oSearchField.getField() + ":" + oSearchField.getValue();
            }
            else
            {
              sQuery += oSearchField.getField() + ":" + oSearchField.getValue() + " AND ";
            }
          }
          oMultiFieldQueryParser = new MultiFieldQueryParser(lsFields, new StandardAnalyzer(CharArraySet.EMPTY_SET));
          oQuery = oMultiFieldQueryParser.parse(sQuery);
        }

        TopDocs oTopDocs = oSearcher.search(oQuery, Integer.MAX_VALUE);
        if (!bLookingForDuplicates)
        {
          String sStatus = "Searching Index " + m_oIndex.getIndexName() + " returned " + oTopDocs.totalHits + " results (Query: " + oQuery + ")";
          System.out.println(sStatus);
          setStatus(sStatus);
        }
        else
        {
          lDuplicateSearch += oTopDocs.totalHits;
        }
        //int iMax = Math.min(1000, oTopDocs.totalHits);
        for (int i = 0; i < oTopDocs.totalHits; i++)
        {
          if (!m_bKeepAlive)
          {
            break;
          }
          Document oDocument = oSearcher.doc(oTopDocs.scoreDocs[i].doc);
          eDocument eDoc = eDocument.from(oDocument);
          if (new File(eDoc.sFileAbsolutePath).exists())
          {
            lsResults.add(eDocument.from(oDocument));
          }
          else
          {
            m_oIndex.deleteFile(new File(eDoc.sFileAbsolutePath));
//            if (iMax + 1 < oTopDocs.totalHits)
//            {
//              iMax++;
//            }
          }
        }
      }
      catch (Exception ex)
      {
        Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
        ex.printStackTrace();
      }
    }
    else
    {
      System.err.println("Index Reader is null");
    }
    return lsResults;
  }

  public void close()
  {
    try
    {
      if (m_oIndexReader != null)
      {
        m_oIndexReader.close();
        m_oIndexReader = null;
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public void setStatus(String sStatus)
  {
    setChanged();
    notifyObservers(sStatus);
  }
  
  public void terminate()
  {
    m_bKeepAlive = false;
  }
}
