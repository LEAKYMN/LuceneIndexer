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
package tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author philip
 */
public class cTest
{

  public static void main(String[] args)
  {
    HashMap<String, String> oDeviceMounts = new HashMap();
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
              oDeviceMounts.put(line, "");
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
    
    Set<String> oKeySet = oDeviceMounts.keySet();
    Iterator<String> oIterator = oKeySet.iterator();
    while (oIterator.hasNext())
    {
      String sDevice = oIterator.next();
      String sMountPoint = oDeviceMounts.get(sDevice);
      
      if (sMountPoint.isEmpty())
      {
        try
        {
          String sDev = sDevice.substring(sDevice.lastIndexOf("/")+1);
          sMountPoint = "/home/$USER/" + sDev;
          String sCommand = "mkdir " + sMountPoint;
          System.out.println(sCommand);
          Process p = Runtime.getRuntime().exec(sCommand);
          p.waitFor();

          sCommand = "sudo mount " + sDevice + " " + sMountPoint;
          System.out.println(sCommand);
          p = Runtime.getRuntime().exec(sCommand);
          p.waitFor();
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
}
