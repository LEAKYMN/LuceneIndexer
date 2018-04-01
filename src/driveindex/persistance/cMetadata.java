
package driveindex.persistance;

import driveindex.lucene.cLuceneIndexReader;
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
