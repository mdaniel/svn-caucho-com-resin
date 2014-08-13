package com.caucho.resin.admin.service;

import javax.inject.Inject;
import javax.management.MBeanServer;

import com.caucho.jmx.server.MeterGraphInfo;
import com.caucho.jmx.server.MeterGraphPageInfo;
import com.caucho.jmx.server.MeterGraphSectionInfo;
import com.caucho.management.server.BaselineQueryResult;
import com.caucho.management.server.DownTime;
import com.caucho.management.server.StatServiceMXBean;
import com.caucho.management.server.StatServiceValue;

import com.caucho.baratine.Remote;
import io.baratine.core.OnActive;
import io.baratine.core.Service;

@Remote
@Service("/stat")
public class ResinStatService
{
  @Inject
  private MBeanServer _mbeanServer;

  private StatServiceMXBean _statService;

  @OnActive
  public void onStart()
  {
    _statService = MBeanUtil.find("caucho:type=StatService", StatServiceMXBean.class);
  }

  public String[] getNames(String serverId)
  {
    StatServiceMXBean statService = getService(serverId);

    return statService.statisticsNames();
  }

  private StatServiceMXBean getService(String serverId)
  {
    StatServiceMXBean statService = _statService;

    if (serverId != null) {
      statService = MBeanUtil.find("caucho:type=StatService", StatServiceMXBean.class, serverId);
    }

    return statService;
  }

  public long getSamplePeriod(String serverId)
  {
    return getService(serverId).getSamplePeriod();
  }

  public StatServiceValue []getData(String serverId,
                                    String name,
                                    long beginTime,
                                    long endTime,
                                    long stepTime)
  {
    System.err.println(getClass().getSimpleName() + ".getData0: " + name + " . " + beginTime + " . " + endTime + " . " + stepTime);

    StatServiceMXBean service = getService(serverId);

    return service.statisticsData(name, beginTime, endTime, stepTime);
  }

  public MeterGraph []getMeterGraphs(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getMeterGraphs0");

    try {
      StatServiceMXBean service = getService(serverId);

      MeterGraphInfo []infoList = service.getMeterGraphs();
      MeterGraph []graphs = new MeterGraph[infoList.length];

      for (int i = 0; i < infoList.length; i++) {
        MeterGraphInfo info = infoList[i];

        graphs[i] = new MeterGraph(info);
      }

      return graphs;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public MeterGraphPage getMeterGraphPage(String serverId, String name)
  {
    System.err.println(getClass().getSimpleName() + ".getMeterGraphPage0: " + name);

    try {
      StatServiceMXBean service = getService(serverId);

      MeterGraphPageInfo info = service.getMeterGraphPage(name);

      if (info == null) {
        return null;
      }

      return new MeterGraphPage(info);
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public MeterGraphPage []getMeterGraphPages(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getMeterGraphPages0");

    try {
      StatServiceMXBean service = getService(serverId);
      MeterGraphPageInfo []infoList = service.getMeterGraphPages();

      // MeterGraphPageInfo is a proxy, need to return a simple object
      MeterGraphPage []pages = new MeterGraphPage[infoList.length];

      for (int i = 0; i < infoList.length; i++) {
        MeterGraphPageInfo info = infoList[i];

        pages[i] = new MeterGraphPage(info);
      }

      return pages;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public BaselineQueryResult getBaseline(String serverId,
                                         String name,
                                         long beginTime,
                                         long endTime,
                                         int minSampleSize)
  {
    StatServiceMXBean service = getService(serverId);

    return service.getBaseline(name, beginTime, endTime, minSampleSize);
  }

  public DownTime []getDownTimes(String serverId,
                                 long start,
                                 long end)
  {
    System.err.println(getClass().getSimpleName() + ".getDownTimes0");

    StatServiceMXBean service = getService(serverId);

    if (end < 0) {
      end = Long.MAX_VALUE / 2;
    }

    DownTime []downTimes = service.getDownTimes(-1, start, end);

    for (DownTime downTime : downTimes) {
      System.err.println(getClass().getSimpleName() + ".getDownTimes1: " + downTime.getStartTime());
    }

    return downTimes;
  }

  public long []getStartTimes(String serverId, long beginTime, long endTime)
  {
    System.err.println(getClass().getSimpleName() + ".getStartTimes0");

    StatServiceMXBean service = getService(serverId);

    long []startTimes = service.getStartTimes(-1, beginTime, endTime);

    return startTimes;
  }

  public static class MeterGraphPage {
    String _name;
    int _columns;
    long _period;

    boolean _isSummary;
    boolean _isLog;
    boolean _isHeapDump;
    boolean _isProfile;
    boolean _isThreadDump;
    boolean _isJmxDump;

    MeterGraphSection []_meterSections;
    MeterGraph []_meterGraphs;

    public MeterGraphPage(String name, int columns, long period,
                          boolean isSummary, boolean isLog, boolean isHeapDump,
                          boolean isProfile, boolean isThreadDump, boolean isJmxDump,
                          MeterGraphSection []meterSections, MeterGraph []meterGraphs)
    {
      _name = name;
      _columns = columns;
      _period = period;

      _isSummary = isSummary;
      _isLog = isLog;
      _isHeapDump = isHeapDump;

      _isProfile = isProfile;
      _isThreadDump = isThreadDump;
      _isJmxDump = isJmxDump;

      _meterSections = meterSections;
      _meterGraphs = meterGraphs;
    }

    public MeterGraphPage(MeterGraphPageInfo info)
    {
      _name = info.getName();
      _columns = info.getColumns();
      _period = info.getPeriod();

      _isSummary = info.isSummary();
      _isLog = info.isLog();
      _isHeapDump = info.isHeapDump();

      _isProfile = info.isProfile();
      _isThreadDump = info.isThreadDump();
      _isJmxDump = info.isJmxDump();

      MeterGraphSectionInfo []sectionInfoList = info.getMeterSections();
      _meterSections = MeterGraphSection.create(sectionInfoList);

      MeterGraphInfo []graphInfoList = info.getMeterGraphs();
      _meterGraphs = MeterGraph.create(graphInfoList);
    }
  }

  public static class MeterGraph {
    String _name;
    String []_meterNames;

    public MeterGraph(String name, String []meterNames)
    {
      _name = name;
      _meterNames = meterNames;
    }

    public MeterGraph(MeterGraphInfo info)
    {
      _name = info.getName();
      _meterNames = info.getMeterNames();
    }

    public static MeterGraph []create(MeterGraphInfo []infoList)
    {
      MeterGraph []graphList = new MeterGraph[infoList.length];

      for (int i = 0; i < infoList.length; i++) {
        MeterGraphInfo info = infoList[i];

        graphList[i] = new MeterGraph(info);
      }

      return graphList;
    }
  }

  public static class MeterGraphSection {
    String _name;
    MeterGraph []_meterGraphs;

    public MeterGraphSection(String name, MeterGraph []meterGraphs)
    {
      _name = name;
      _meterGraphs = meterGraphs;
    }

    public static MeterGraphSection []create(MeterGraphSectionInfo []infoList)
    {
      MeterGraphSection []sectionList = new MeterGraphSection[infoList.length];

      for (int i = 0; i < infoList.length; i++) {
        MeterGraphSectionInfo info = infoList[i];

        String name = info.getName();
        MeterGraph []meterGraphs = MeterGraph.create(info.getMeterGraphs());

        sectionList[i] = new MeterGraphSection(name, meterGraphs);
      }

      return sectionList;
    }
  }
}
