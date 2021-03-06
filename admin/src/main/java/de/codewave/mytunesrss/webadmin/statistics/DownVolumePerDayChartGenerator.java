/*
 * Copyright (c) 2011. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.webadmin.statistics;

import de.codewave.mytunesrss.statistics.DownloadEvent;
import de.codewave.mytunesrss.statistics.StatEventType;
import de.codewave.mytunesrss.statistics.StatisticsEvent;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DownVolumePerDayChartGenerator extends TimeSeriesCharGenerator implements ReportChartGenerator {
    @Override
    public JFreeChart generate(Map<Day, List<StatisticsEvent>> eventsPerDay, ResourceBundle bundle) {
        TimeSeries ts = new TimeSeries(getClass().getSimpleName());
        for (Map.Entry<Day, List<StatisticsEvent>> entry : eventsPerDay.entrySet()) {
            long volume = 0;
            for (StatisticsEvent event : entry.getValue()) {
                volume += ((DownloadEvent)event).myBytes;
            }
            long value = volume / (1024 * 1024); // megabyte
            ts.add(entry.getKey(), value);
        }
        TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection(ts);
        return createTimeSeriesChart(timeSeriesCollection, bundle, "statisticsConfigPanel.chart.axisVolumeMib");
    }

    @Override
    public StatEventType[] getEventTypes() {
        return new StatEventType[] {
                StatEventType.DOWNLOAD
        };
    }

    @Override
    public String toString() {
        return "statisticsConfigPanel.reportType.downvolume";
    }
}
