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
package LuceneIndexer.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import tests.cTest;

/**
 *
 * @author Philip M. Trenwith
 */
public class cLinux 
{
  private static ArrayList<String> m_oDrivesMountedByThisApp = new ArrayList();
  public static File[] getAllDrives()
  {
    HashMap<String, String> oDevices = new HashMap();
    for (char c = 'a'; c <= 'z'; c++)
    {
      for (int i = 1; i <= 10; i++)
      {
        try
        {
          String sCommand = "find /dev/sd" + c + i;
          Process p = Runtime.getRuntime().exec(sCommand);
          p.waitFor();

          BufferedReader reader = new BufferedReader(
                  new InputStreamReader(p.getInputStream()));

          String line = "";
          while ((line = reader.readLine())!= null) 
          {
            if (line.matches(".*\\d"))
            {
              System.out.println("Add device: " + line);
              oDevices.put(line, "");
            }
          }
        }
        catch (IOException ex)
        {
          Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (InterruptedException ex)
        {
          Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    
    int iSize = oDevices.size();
    int iCount = 0;
    File[] loMountPoints = new File[iSize];
    Set<String> lsValues = oDevices.keySet();
    Iterator<String> oIterator = lsValues.iterator();
    while (oIterator.hasNext())
    {
      String sMountPoint = oIterator.next();
      loMountPoints[iCount++] = new File(sMountPoint);
    }
    return loMountPoints;
  }
  
  public static File[] mountAllDrives(boolean bIgnoreRoot)
  {
    File[] lsDrives = getAllDrives();
    HashMap<String, String> oDeviceMountPoints = getDeviceMountPoints();
    for (File oDrive : lsDrives)
    {
      String sDevice = oDrive.getAbsolutePath();
      String sMountPoint = oDeviceMountPoints.get(sDevice);
      
      if (sMountPoint == null || sMountPoint.isEmpty())
      {
        try
        {
          //String sUsername = getUsername();
          String sDev = sDevice.substring(sDevice.lastIndexOf("/")+1);
          sMountPoint = "/home/" + sDev;
          m_oDrivesMountedByThisApp.add(sMountPoint);
          String sCommand = "mkdir " + sMountPoint;
          System.out.println(sCommand);
          Process p = Runtime.getRuntime().exec(sCommand);
          p.waitFor();
          
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(p.getInputStream()));

          String line = "";
          while ((line = reader.readLine())!= null) 
          {
            System.out.println(line);
          }

          sCommand = "sudo mount " + sDevice + " " + sMountPoint;
          System.out.println(sCommand);
          p = Runtime.getRuntime().exec(sCommand);
          p.waitFor();
          
          reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
          while ((line = reader.readLine())!= null) 
          {
            System.out.println(line);
          }
        }
        catch (IOException ex)
        {
          Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (InterruptedException ex)
        {
          Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    ArrayList<String> lsMountPoints = getMountPoints();
    int iSize = lsMountPoints.size();
    if (bIgnoreRoot)
    {
      iSize--;
    }
    
    int iCount = 0;
    File[] loMountPoints = new File[iSize];
    for (String sMountPoint: lsMountPoints)
    {
      if (bIgnoreRoot)
      {
        if (!sMountPoint.equals("/"))
        {
          loMountPoints[iCount++] = new File(sMountPoint);
        }
      }
      else
      {
        loMountPoints[iCount++] = new File(sMountPoint);
      }
    }
    return loMountPoints;
  }
 
  public static HashMap<String, String> getDeviceMountPoints()
  {
    HashMap<String, String> oDeviceMounts = new HashMap();
    try
    {
      String sCommand = "df -h";
      Process p = Runtime.getRuntime().exec(sCommand);
      p.waitFor();

      BufferedReader reader = new BufferedReader(
              new InputStreamReader(p.getInputStream()));

      String line = "";
      while ((line = reader.readLine())!= null) 
      {
        if (line.startsWith("/dev"))
        {
          int iSpace = line.indexOf(" ");
          String sDevice = line.substring(0,iSpace);
          
          int iLastSpace = line.lastIndexOf(" /")+1;
          String sMountPoint = line.substring(iLastSpace);
          
          oDeviceMounts.put(sDevice, sMountPoint);
        }
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (InterruptedException ex)
    {
      Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return oDeviceMounts;
  }
  
  public static ArrayList<String> getMountPoints()
  {
    ArrayList<String> oDeviceMounts = new ArrayList();
    try
    {
      String sCommand = "df -h";
      Process p = Runtime.getRuntime().exec(sCommand);
      p.waitFor();

      BufferedReader reader = new BufferedReader(
              new InputStreamReader(p.getInputStream()));

      String line = "";
      while ((line = reader.readLine())!= null) 
      {
        if (line.startsWith("/dev"))
        {          
          int iLastSpace = line.lastIndexOf(" /")+1;
          String sMountPoint = line.substring(iLastSpace);
          
          oDeviceMounts.add(sMountPoint);
        }
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (InterruptedException ex)
    {
      Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    return oDeviceMounts;
  }
  
  public static String getUsername()
  {
    // this is no where near optimal but will do for now.
    String sUsername = "";
    try
    {
      String sCommand = "whoami";
      Process p = Runtime.getRuntime().exec(sCommand);
      p.waitFor();

      BufferedReader reader = new BufferedReader(
              new InputStreamReader(p.getInputStream()));

      String line = "";
      while ((line = reader.readLine())!= null) 
      {
        sUsername = line;
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (InterruptedException ex)
    {
      Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    return sUsername;
  }
  
  public static void unmountMountedDrives()
  {
    for (String sMountPoint : m_oDrivesMountedByThisApp)
    {
      try
      {
        String sCommand = "sudo umount " + sMountPoint;
        Process p = Runtime.getRuntime().exec(sCommand);
        p.waitFor();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()));

        String line = "";
        while ((line = reader.readLine())!= null) 
        {
          System.out.println(line);
        }
        
        sCommand = "sudo rm -r " + sMountPoint;
        p = Runtime.getRuntime().exec(sCommand);
        p.waitFor();

        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        line = "";
        while ((line = reader.readLine())!= null) 
        {
          System.out.println(line);
        }
      }
      catch (IOException ex)
      {
        Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (InterruptedException ex)
      {
        Logger.getLogger(cTest.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}