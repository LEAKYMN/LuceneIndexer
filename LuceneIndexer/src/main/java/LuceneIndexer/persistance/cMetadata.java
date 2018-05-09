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
package LuceneIndexer.persistance;

import LuceneIndexer.lucene.cLuceneIndexReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Philip M. Trenwith
 */
public class cMetadata 
{
  public final static String m_sKEY_LAST_SCAN = "lastscan";
  public final static String m_sKEY_STATUS    = "status";
  public final static String m_sKEY_INDEXED   = "indexed";
  public final static String m_sKEY_DURATION  = "duration";
  
  private static SimpleDateFormat g_DF = new SimpleDateFormat("dd MMMMMM yyyy HH:mm:ss");
  private final Object m_oCommitLock = new Object();
  private Properties m_oProperties = new Properties();
  private String m_sMetadataFileName = "DriveIndexMetadata";
  private String m_sMetadataFileExtension = "properties";
  private String m_sMetadataFile = "DriveIndexMetadata.properties";
  
  public cMetadata(String sMountPoint)
  {
    m_sMetadataFile = m_sMetadataFileName + "_" + sMountPoint + "." + m_sMetadataFileExtension;
  }
  
  public boolean exists()
  {
    return new File(m_sMetadataFile).exists();
  }
  
  private void loadFromFile()
  {
    InputStream oFileInputStream;
    try 
    {
      File oFile = new File(m_sMetadataFile);
      oFileInputStream = new FileInputStream( oFile );
    }
    catch (Exception ex) 
    {
      oFileInputStream = null; 
    }

    try 
    {
      if ( oFileInputStream == null ) 
      {
        // Try loading from classpath
        oFileInputStream = getClass().getResourceAsStream(m_sMetadataFile);
      }

      // Try loading properties from the file (if found)
      m_oProperties.load(oFileInputStream);
    }
    catch ( Exception e ) { }
    finally
    {
      if (oFileInputStream != null)
      {
        try
        {
          oFileInputStream.close();
        }
        catch (IOException ex)
        {
          Logger.getLogger(cMetadata.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  public String getPropertyValue(String sKey)
  {
    loadFromFile();
    return m_oProperties.getProperty(sKey);
  }

  public void setPropertyValue(String sKey, String sValue, boolean bCommit)
  {
    try
    {      
      System.out.println("Updating key: " + sKey + " = " + sValue);
      m_oProperties.setProperty(sKey, sValue);
      if (bCommit)
      {
        commit();
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void removeProperty(String sTempId)
  {
    System.out.println("Removing property: " + sTempId);
    m_oProperties.remove(sTempId);
    commit();
  }

  public void commit()
  {
    Date time = new GregorianCalendar().getTime();
    synchronized (m_oCommitLock)
    {
      File f = new File(m_sMetadataFile);
      try
      {
        OutputStream out = new FileOutputStream( f );
        m_oProperties.store(out, "updated at: " + g_DF.format(time));
      }
      catch (Exception ex)
      {
        Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
      }
      finally
      {
        m_oProperties.clear();
      }
    }
  }

  public void delete()
  {
    synchronized (m_oCommitLock)
    {
      File f = new File(m_sMetadataFile);
      System.out.println("Deleting metadata: " + m_sMetadataFile);
      f.delete();
    }
  }
}
