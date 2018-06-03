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
package LuceneIndexer.scheduling;

import LuceneIndexer.drives.cDriveMediator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Philip M. Trenwith
 */
public class cSchedular
{
  private ScheduledExecutorService m_oScheduler = null;
  private int m_iPeriod_hr = 24;
  
  public cSchedular(int iThreadCount)
  {
    m_oScheduler = Executors.newScheduledThreadPool(iThreadCount);
    
  }
  
  public void runAt(int iHourOfDay_24)
  {
    LocalDateTime oLocalTimeNow = LocalDateTime.now();
    ZoneId oCurrentTimeZone = ZoneId.systemDefault();
    ZonedDateTime oTimeNowZoned = ZonedDateTime.of(oLocalTimeNow, oCurrentTimeZone);
    ZonedDateTime oDesiredTimeZoned ;
    oDesiredTimeZoned = oTimeNowZoned.withHour(iHourOfDay_24).withMinute(0).withSecond(0);
    if(oTimeNowZoned.compareTo(oDesiredTimeZoned) > 0)
    {
      oDesiredTimeZoned = oDesiredTimeZoned.plusDays(1);
    }
    Duration duration = Duration.between(oTimeNowZoned, oDesiredTimeZoned);
    long initalDelay = duration.getSeconds();
    
    final Runnable oScanner = new Runnable() 
    {
      @Override
      public void run() 
      { 
        cDriveMediator.instance().scanComputer();
      }
    };
    
    System.out.println("Scheduled to run at: " + oDesiredTimeZoned + " (In: " + initalDelay + " seconds)");
    m_oScheduler.scheduleAtFixedRate(oScanner, initalDelay, m_iPeriod_hr*60*60, TimeUnit.SECONDS);
  }
  
  public void terminate()
  {
    m_oScheduler.shutdown();
  }
  
}
