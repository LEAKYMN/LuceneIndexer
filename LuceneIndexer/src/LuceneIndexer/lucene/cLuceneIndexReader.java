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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
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
  private long m_lMaxDocs = 0;
  private ExecutorService m_oExecutorService;
  private AtomicInteger m_lDuplicateSearch = new AtomicInteger(0);
  private AtomicInteger m_oAlive = new AtomicInteger(0);
  private volatile HashMap<String, ArrayList<eDocument>> m_oDuplicateResults = new HashMap();
  private DecimalFormat m_oDecimalFormat = new DecimalFormat("#.######");
  private IndexReader m_oIndexReader = null;
  private boolean m_bIsOpen = false;
  private cIndex m_oIndex;
  private Thread m_tProgressThread;
  private boolean m_bKeepAlive = true;
  private volatile boolean m_bDuplicationSearch = false; 
  private final Object m_oDuplicateLock = new Object();
  public cLuceneIndexReader(cIndex oIndex)
  {
    m_oIndex = oIndex;
    addObserver(cInjector.getInjector().getInstance(cMainLayoutController.class));
    
    m_tProgressThread = new Thread(()-> 
    {
      while (m_bKeepAlive)
      {
        if (!m_bDuplicationSearch)
        {
          synchronized (m_oDuplicateLock)
          {
            try
            {
              System.out.println("lDuplicateSearch: " + m_lDuplicateSearch + " (lMaxDocs) " + m_lMaxDocs + " Alice: " + m_oAlive.get());
              String sStatus = "Looking for duplicates: " + " " + m_lDuplicateSearch.get()/(m_lMaxDocs/100*100f)*100  + " %";
              setStatus(sStatus);
              System.out.println("Entering wait state");
              m_oDuplicateLock.wait();
            }
            catch (InterruptedException ex)
            { }
          }
        }
        
        // m_oDecimalFormat.format(lDuplicateSearch/(lMaxDocs/100*100f))
        System.out.println("lDuplicateSearch: " + m_lDuplicateSearch + " (lMaxDocs) " + m_lMaxDocs + " Alice: " + m_oAlive.get());
        String sStatus = "Looking for duplicates: " + " " + m_lDuplicateSearch.get()/(m_lMaxDocs/100*100f)*100  + " %";
        setStatus(sStatus);
        
        try
        {
          Thread.sleep(500);
        }
        catch (InterruptedException ex)
        { }
      }
    });
    m_tProgressThread.setName("ProgressThread");
    m_tProgressThread.start();
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
    synchronized (m_oDuplicateLock)
    {
      m_oDuplicateLock.notify();
    }
    m_oDuplicateResults = new HashMap();
    int iThreads = cConfig.instance().getScanThreads();
    ThreadFactory tFactory = (Runnable runnable) ->
    {
      Thread thread = new Thread(runnable);
      thread.setName("DuplicationScan-Thread");
      thread.setDaemon(true);
      thread.setPriority(3);
      return thread;
    };
    m_oAlive = new AtomicInteger(0);
    m_oExecutorService = Executors.newFixedThreadPool(iThreads, tFactory);
    if (m_oIndexReader != null)
    {
      m_lDuplicateSearch = new AtomicInteger(0);
      m_lMaxDocs = m_oIndexReader.maxDoc();
      for (int i = 0; i < m_lMaxDocs; i++)
      {
        if (!m_bKeepAlive)
        {
          break;
        }
        final int fi = i;
        m_oAlive.getAndIncrement();
        m_oExecutorService.submit(()-> 
        {
          try
          {
            Document oDocument = m_oIndexReader.document(fi);
            eDocument eDoc = eDocument.from(oDocument);
            if (!m_oDuplicateResults.containsKey(eDoc.sFileHash) && eDoc.sFileHash != null)
            {
              eSearchField oSearchField = new eSearchField(eDocument.TAG_Hash, eDoc.sFileHash);
              ArrayList<eSearchField> lsSearchFields = new ArrayList<>(Arrays.asList(oSearchField));

              ArrayList<eDocument> oDocs = search(lsSearchFields, true, false, true);
              if (oDocs.size() > 1)
              {
                ArrayList<eDocument> lsReturn = oDocs;
                m_oDuplicateResults.put(eDoc.sFileHash, lsReturn);
              }
            }
            else if (eDoc.sFileHash == null)
            {
              m_lDuplicateSearch.incrementAndGet();
            }
          }
          catch (IOException ex)
          {
            Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
          }
          finally
          {
            m_oAlive.decrementAndGet();
          }
        });        
      }
      
      while (m_oAlive.get() > 0)
      {

      }
    }
    m_bDuplicationSearch = false;
    return m_oDuplicateResults;
  }

  public ArrayList<eDocument> search(ArrayList<eSearchField> lsSearchFields, boolean bWholeWords, boolean bCaseSensitive,
      boolean bLookingForDuplicates)
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
          m_lDuplicateSearch.addAndGet(oTopDocs.totalHits);
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
    if (m_oExecutorService != null)
    {
      m_oExecutorService.shutdownNow();
    }
  }
}
