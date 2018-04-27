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
import LuceneIndexer.drives.cDrive;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
  private IndexReader m_oIndexReader = null;
  private boolean m_bIsOpen = false;
  private cIndex m_oIndex;
  
  public cLuceneIndexReader(cIndex oIndex)
  {
    m_oIndex = oIndex;
    addObserver(cInjector.getInjector().getInstance(cMainLayoutController.class));
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
      for (int i=0; i<iCeiling; i++) 
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
  
  public ArrayList<eDocument> search(ArrayList<eSearchField> lsSearchFields, boolean bWholeWords, boolean bCaseSensitive)
  {
    if (m_oIndexReader == null)
    {
      open();
    }
    
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
            for (int i=0; i<iFieldLength; i++)
            {
                eSearchField oSearchField = lsSearchFields.get(i);
                String[] lsSplit = oSearchField.getValue().split(" ");
                PhraseQuery.Builder builder = new PhraseQuery.Builder();
                for (int j=0; j<lsSplit.length; j++)
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
            for (int i=0; i<iFieldLength; i++)
            {
              eSearchField oSearchField = lsSearchFields.get(i);
              lsFields[i] = oSearchField.getField();
              if (i == iFieldLength-1)
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
        String sStatus = "Searching Index " + m_oIndex.getIndexName() + " returned " + oTopDocs.totalHits + " results (Query: " + oQuery +")";
        System.out.println(sStatus);
        setStatus(sStatus);
        int iMax = Math.min(1000, oTopDocs.totalHits);
        for (int i=0; i<iMax; i++)
        {
          Document oDocument = oSearcher.doc(oTopDocs.scoreDocs[i].doc);
          eDocument eDoc = eDocument.from(oDocument);
          if (new File(eDoc.sFileAbsolutePath).exists())
          {
            lsResults.add(eDocument.from(oDocument));
          }
          else
          {
            m_oIndex.deleteFile(new File(eDoc.sFileAbsolutePath));
            if (iMax+1 < oTopDocs.totalHits)
            {
              iMax++;
            }
          }
        }
      }
      catch (Exception ex) 
      {
        Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
      }
      finally 
      {
        close();
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
      m_oIndexReader.close();
      m_oIndexReader = null;
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
}
