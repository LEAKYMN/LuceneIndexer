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

import LuceneIndexer.cConfig;
import LuceneIndexer.drives.cDriveMediator;
import LuceneIndexer.injection.cInjector;
import LuceneIndexer.ui.fx.cMainLayoutController;
import com.google.inject.Injector;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Philip M. Trenwith
 */
public class cSchedular
{
  private final SimpleDateFormat m_oTimeFormat = new SimpleDateFormat("HH:mm:ss");
  private ScheduledExecutorService m_oScheduler = null;
  private ZonedDateTime oDesiredTimeZoned;
  private int m_iPeriod_hr = 24;
  private long m_iInitalDelay;
  
  
  
  public cSchedular(int iThreadCount)
  {
    m_oScheduler = Executors.newScheduledThreadPool(iThreadCount);
    
  }
  
  public void runAt(final int iHourOfDay_24)
  {
    initNextRun(iHourOfDay_24);
    
    final Runnable oScanner = new Runnable() 
    {
      @Override
      public void run() 
      { 
        cDriveMediator.instance().scanComputer();
      }
    };
    
    m_oScheduler.scheduleAtFixedRate(oScanner, m_iInitalDelay, m_iPeriod_hr*60*60, TimeUnit.SECONDS);
    
    Injector oInjector = cInjector.getInjector();
    cMainLayoutController oMainController = oInjector.getInstance(cMainLayoutController.class);
    System.out.println("Scheduled to run at: " + oDesiredTimeZoned);
    oMainController.setScheduleLabelText("Scheduled to run at: " + oDesiredTimeZoned);
    if (cConfig.instance().getCountdown())
    {
      Timer oTimer = new Timer();
      oTimer.scheduleAtFixedRate(new TimerTask() 
      {
        @Override
        public void run() 
        {
          m_iInitalDelay --;
          if (m_iInitalDelay <= 0)
          {
            initNextRun(iHourOfDay_24);
          }
          long iMillis = m_iInitalDelay*1000;

          String sFormattedDelay = String.format("%02d:%02d:%02d", 
          TimeUnit.MILLISECONDS.toHours(iMillis),
          TimeUnit.MILLISECONDS.toMinutes(iMillis) -  
          TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(iMillis)),
          TimeUnit.MILLISECONDS.toSeconds(iMillis) - 
          TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(iMillis)));

          oMainController.setScheduleLabelText("Scheduled to run at: " + oDesiredTimeZoned + " (In: " + sFormattedDelay + ")");
        }
      }, 0, 1000);
    }
  }
  
  private void initNextRun(int iHourOfDay_24)
  {
    LocalDateTime oLocalTimeNow = LocalDateTime.now();
    ZoneId oCurrentTimeZone = ZoneId.systemDefault();
    ZonedDateTime oTimeNowZoned = ZonedDateTime.of(oLocalTimeNow, oCurrentTimeZone);
    oDesiredTimeZoned = oTimeNowZoned.withHour(iHourOfDay_24).withMinute(13).withSecond(0);
    if(oTimeNowZoned.compareTo(oDesiredTimeZoned) > 0)
    {
      oDesiredTimeZoned = oDesiredTimeZoned.plusDays(1);
    }
    Duration duration = Duration.between(oTimeNowZoned, oDesiredTimeZoned);
    m_iInitalDelay = duration.getSeconds();
  }
  
  public void terminate()
  {
    m_oScheduler.shutdownNow();
  }
}
