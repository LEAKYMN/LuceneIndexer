
package driveindex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Philip M. Trenwith
 */
public class cConfig 
{
  private static volatile cConfig m_oInstance = null;
  private File m_oConfigFile = null;
  private static String sConfigKey = "DriveIndex.Config";
  private static String sConfigKeyValue = "DriveIndex.xml";
  
  private String m_sIndexLocation = "";
  private String m_sDefaultCategory = "";
  private HashMap<String, String> m_oCategoryForFile = new HashMap();
  private HashSet<String> m_oScanDrives = new HashSet();
  private NodeList m_aoNodes = null;
  
  public static cConfig instance()
  {
    cConfig oInstance = cConfig.m_oInstance;
    if (oInstance == null)
    {
      synchronized (cConfig.class)
      {
        oInstance = cConfig.m_oInstance;
        if (oInstance == null)
        {
          cConfig.m_oInstance = oInstance = new cConfig();
        }
      }
    }
    return oInstance;
  }
  
  private cConfig()
  {
    loadConfig();
  }
  
  private File getConfigFile()
  {
    String sFilename = System.getProperty(sConfigKey, sConfigKeyValue);
    m_oConfigFile = new File(sFilename);
    if (!m_oConfigFile.exists())
    {
      saveConfig();
    }
    return m_oConfigFile;
  }
  
  private void loadConfig()
  {
    try
    {
      m_oConfigFile = getConfigFile();
   
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document oDocument = dBuilder.parse(m_oConfigFile);      
      Element oDocumentElement = oDocument.getDocumentElement();
      m_aoNodes = oDocumentElement.getChildNodes();
      
      Element oIndexElement = getElement("Index");
      m_sIndexLocation = getChildElement(oIndexElement, "Location").getTextContent();
      
      Element oDrivesElement = getElement("Drives");
      ArrayList<Element> lsDriveTypes = getChildElements(oDrivesElement, "Type");
      for (Element oDriveType :lsDriveTypes)
      {
        boolean bScan = Boolean.parseBoolean(""+oDriveType.getAttribute("Scan"));
        if (bScan)
        {
          m_oScanDrives.add(oDriveType.getTextContent());
        }
      }
      
      Element oCategoriesElement = getElement("Categories");
      m_sDefaultCategory = oCategoriesElement.getAttribute("Default");
      ArrayList<Element> lsCategories = getChildElements(oCategoriesElement, "Category");
      for (Element oCategory :lsCategories)
      {
        String sName = oCategory.getAttribute("Name");
        String sExtensions = oCategory.getTextContent();
        String[] lsExtensions = sExtensions.split(",");
        for (String sExtension: lsExtensions)
        {
          m_oCategoryForFile.put(sExtension, sName);
        }
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void saveConfig()
  {
    Writer oWriter = null;
    try
    {
      oWriter = new FileWriter(m_oConfigFile);
      //XMLConfiguration oConfig = new XMLConfiguration();
      
      //oConfig.write(oWriter);
    }
    catch (Exception ex)
    {
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally
    {
      if (oWriter != null)
      {
        try 
        {
          oWriter.flush();
          oWriter.close();
        }
        catch (IOException ex) 
        {
          Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }
  
  public Element getElement(String sTagName)
  {
    for (int i = 0; i < m_aoNodes.getLength(); ++i)
    {
      Node oNode = m_aoNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oChild = (Element) oNode;
        if(oChild.getTagName().equals(sTagName))
        {
          return oChild;
        }
      }
    }
    return null;
  }
  
  public ArrayList<Element> getElements(String sTagName)
  {
    ArrayList<Element> lsResults = new ArrayList();
    
    for (int i = 0; i < m_aoNodes.getLength(); ++i)
    {
      Node oNode = m_aoNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oElement = (Element) oNode;
        if(oElement.getTagName().equals(sTagName))
        {
          lsResults.add(oElement);
        }
      }
    } 
    return lsResults;
  }
  
  public Element getChildElement(Element oParentElement, String sTagName)
  {
    NodeList oChildNodes = oParentElement.getChildNodes();
    for (int i = 0; i < oChildNodes.getLength(); ++i)
    {
      Node oNode = oChildNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oChild = (Element) oNode;
        if(oChild.getTagName().equals(sTagName))
        {
          return oChild;
        }
      }
    }
    return null;
  }
  
  public ArrayList<Element> getChildElements(Element oParentElement, String sTagName)
  {
    ArrayList<Element> lsResults = new ArrayList();
    NodeList oChildNodes = oParentElement.getChildNodes();
    for (int i = 0; i < oChildNodes.getLength(); ++i)
    {
      Node oNode = oChildNodes.item(i);
      if (oNode instanceof Element)
      {
        Element oElement = (Element) oNode;
        if(oElement.getTagName().equals(sTagName))
        {
          lsResults.add(oElement);
        }
      }
    } 
    return lsResults;
  }
  
  public String getIndexLocation()
  {
    return m_sIndexLocation;
  }
  
  public String getCategory(File oFile)
  {
    String ext = FilenameUtils.getExtension(oFile.getAbsolutePath());
    if (m_oCategoryForFile.containsKey(ext))
    {
      return m_oCategoryForFile.get(ext);
    }
    else
    {
      return m_sDefaultCategory;
    }
  }
  
  public boolean getScanDriveType(String sType)
  {
    return m_oScanDrives.contains(sType);
  }
}
