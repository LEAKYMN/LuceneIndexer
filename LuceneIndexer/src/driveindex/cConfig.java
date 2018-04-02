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
package driveindex;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
    try
    {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      // root elements
      Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement("LuceneIndex");
      doc.appendChild(rootElement);

      Element oIndex = doc.createElement("Index");
      rootElement.appendChild(oIndex);
      Element oLocation = doc.createElement("Location");
      oIndex.appendChild(oLocation);
      oLocation.setTextContent("home/$USER/index/");
      
      Element oDrives = doc.createElement("Drives");
      rootElement.appendChild(oDrives);
      Element oType = doc.createElement("Type");
      oDrives.appendChild(oType);
      oType.setAttribute("Scan", "true");
      
      Element oCategories = doc.createElement("Categories");
      oCategories.setAttribute("Default", "File");
      rootElement.appendChild(oCategories);
      Element oCategory = doc.createElement("Category");
      oCategories.appendChild(oCategory);
      oCategory.setAttribute("Name", "File");
      oCategory.setTextContent("*.*");
      
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(m_oConfigFile);
      transformer.transform(source, result);
    }
    catch (Exception ex)
    {
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
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
