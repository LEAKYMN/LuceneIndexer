/*
 * Copyright (C) 2018 Philip
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
import LuceneIndexer.drives.cDrive;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 *
 * @author Philip M. Trenwith
 */
public class cIndex
{
  private static TreeMap<Character, cIndex> m_lsIndexes = new TreeMap();
  private cLuceneIndexWriter m_oLuceneIndexWriter = null;
  private cLuceneIndexReader m_oLuceneIndexReader = null;
  
  private File m_oDrive = null;
  private final char m_cDriveLetter;
  private Path m_oPath;
  
  public static ArrayList<eDocument> search(char _Index, ArrayList<eSearchField> lsSearchFields, 
          boolean wholeWords, boolean caseSensitive)
  {
    ArrayList<eDocument> lsResults = new ArrayList<>();
    cIndex oIndex = m_lsIndexes.get(_Index);
    lsResults.addAll(oIndex.search(lsSearchFields, wholeWords, caseSensitive));
    return lsResults;
  }
  
  public static ArrayList<eDocument> searchAll(ArrayList<eSearchField> lsSearchFields, 
          boolean wholeWords, boolean caseSensitive)
  {
    ArrayList<eDocument> lsResults = new ArrayList<>();
    Iterator<cIndex> oIterator = m_lsIndexes.values().iterator();
    oIterator.forEachRemaining(oIndex -> 
    {
      lsResults.addAll(oIndex.search(lsSearchFields, wholeWords, caseSensitive));
    });
    return lsResults;
  }
  
  public static HashMap<String, ArrayList<eDocument>> findDuplicates(char _Index)
  {
    cIndex oIndex = m_lsIndexes.get(_Index);
    return oIndex.findDuplicateDocuments();
  }
  
  public static void closeIndexs()
  {
    Iterator<cIndex> oIterator = m_lsIndexes.values().iterator();
    oIterator.forEachRemaining(oIndex -> 
    {
      oIndex.close();
      oIndex.terminate();
    });
  }
  
  public cIndex(File oDrive, cDrive oDriveScanner)
  {
    m_oDrive = oDrive;
    String sDriveLetter = m_oDrive.getPath().substring(0,1);
    m_cDriveLetter = sDriveLetter.toCharArray()[0];
    File oFile = new File(cConfig.instance().getIndexLocation());
    m_oPath = Paths.get(oFile.getPath(), sDriveLetter);
    System.out.println("Index path set to: " + m_oPath.toFile().getAbsolutePath());
    m_oLuceneIndexWriter = new cLuceneIndexWriter(this);
    m_oLuceneIndexReader = new cLuceneIndexReader(this);
    m_lsIndexes.put(m_cDriveLetter, this);
  }
  
  public boolean indexFile(File oFile, String sFileHash)
  {
    return m_oLuceneIndexWriter.indexFile(oFile, sFileHash);
  }
  
  public boolean deleteFile(File oFile)
  {
    m_oLuceneIndexWriter.open();
    boolean deleteFile = m_oLuceneIndexWriter.deleteFile(oFile);
    m_oLuceneIndexWriter.close();
    return deleteFile;
  }

  public ArrayList<eDocument> search(ArrayList<eSearchField> lsSearchFields, 
          boolean wholeWords, boolean caseSensitive)
  {
    m_oLuceneIndexReader.open();
    ArrayList<eDocument> lsResults = m_oLuceneIndexReader.search(lsSearchFields, wholeWords, caseSensitive, false);
    m_oLuceneIndexReader.close();
    return lsResults;
  }
  
  public HashMap<String, ArrayList<eDocument>> findDuplicateDocuments()
  {
    m_oLuceneIndexReader.open();
    HashMap<String, ArrayList<eDocument>> lsDocuments = m_oLuceneIndexReader.getDuplicateDocuments();
    m_oLuceneIndexReader.close();
    return lsDocuments;
  }
  
  public int getNumberOfDocuments()
  {
    m_oLuceneIndexReader.open();
    int numberOfDocuments = m_oLuceneIndexReader.getNumberOfDocuments();
    m_oLuceneIndexReader.close();
    return numberOfDocuments;
  }

  public ArrayList<eDocument> getTopNDocuments(int n)
  {
    m_oLuceneIndexReader.open();
    ArrayList<eDocument> lsResults = m_oLuceneIndexReader.getTopNDocuments(n);
    m_oLuceneIndexReader.close();
    return lsResults;
  }
  
  public void openWriter()
  {
    m_oLuceneIndexWriter.open();
  }
  
  public void close()
  {
    m_oLuceneIndexWriter.commit();
    m_oLuceneIndexWriter.close();

    m_oLuceneIndexReader.close();
  }

  public String getIndexLocation()
  {
    return m_oPath.toString();
  }
  
  public char getIndexName()
  {
    return m_cDriveLetter;
  }
  
  public void terminate()
  {
    m_oLuceneIndexReader.terminate();
  }
}
