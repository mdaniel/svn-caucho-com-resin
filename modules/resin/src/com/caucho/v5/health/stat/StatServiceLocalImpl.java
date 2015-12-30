/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import io.baratine.core.Startup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthStatus;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.system.RootDirectorySystem;
import com.caucho.v5.health.analysis.HealthAnalyzer;
import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.health.meter.SampleMetadataAware;
import com.caucho.v5.health.stat.StatSystem.JmxItem;
import com.caucho.v5.health.stat.StatSystem.Sample;
import com.caucho.v5.jmx.JmxUtilResin;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.jmx.server.MeterGraphInfo;
import com.caucho.v5.jmx.server.MeterGraphPageInfo;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.management.server.BaselineQueryResult;
import com.caucho.v5.management.server.DownTime;
import com.caucho.v5.management.server.StatServiceMXBean;
import com.caucho.v5.management.server.StatServiceValue;
import com.caucho.v5.profile.MemoryUtil;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.WeakAlarm;
import com.caucho.v5.vfs.Path;

/**
 * statistics
 */
@Startup
public class StatServiceLocalImpl
  implements AlarmListener
{
  private static Logger log = 
    Logger.getLogger(StatServiceLocalImpl.class.getName());
  
  private static final L10N L = new L10N(StatServiceLocalImpl.class);
  
  private final ServerBartender _selfServer;
  
  // XXX: may be static because of singleton issues
  private Lifecycle _lifecycle = new Lifecycle();

  private long _samplePeriod = 60 * 1000;

  private StatDatabase _statDatabase;
  
  @SuppressWarnings("unused")
  private Admin _admin;

  private StatServiceCluster _statService;

  private final AtomicReference<Sample[]> _sampleRef
    = new AtomicReference<Sample[]>(new Sample[0]);

  private final AtomicReference<long[]> _sampleIdRef
    = new AtomicReference<long[]>();

  private Alarm _alarm = new WeakAlarm(this);

  private JniCpuStat _jniCpuStat;
  private MeterBase _jniCpuMeter;

  private JniNetStat _jniNetStat;
  private JniVmStat _jniVmStat;

  private MBeanServer _mbeanServer;
  
  private ArrayList<MeterBase> _cpuMeters
    = new ArrayList<MeterBase>();
  
  private List<HealthAnalyzer> _analyzers
    = new CopyOnWriteArrayList<>();
  
  private ArrayList<MeterGraphInfo> _meterGraphs
    = new ArrayList<>();
  
  private ArrayList<MeterGraphPageInfo> _meterGraphPages
    = new ArrayList<>();

  private boolean _isResinServer;

  private AtomicBoolean _isInit = new AtomicBoolean();
  
  private int _serverIndex;
  
  private long _startTimeSampleId;
  private long _upSampleId;

  // private JvmThreadsAdmin _jvmThreadsStat;

  private StatServiceLocal _statServiceLocal;

  private HealthSubSystem _healthSystem;

  StatServiceLocalImpl()
  {
    _selfServer = BartenderSystem.getCurrentSelfServer();
    Objects.requireNonNull(_selfServer);
    
    _isResinServer = true; // _server.isResinServer();
    _admin = new Admin();

    _mbeanServer = JmxUtilResin.getMBeanServer();

    /* XXX:
    try {
      _jvmThreadsStat = JvmThreadsAdmin.create();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    */

    try {
      MemoryUtil.create();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      _jniCpuStat = JniCpuStat.create();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      _jniNetStat = JniNetStat.create();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      _jniVmStat = JniVmStat.create();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    // JvmThreadsAdmin.create();
  }
  
  /**
   * Sets the sample period
   */
  public void setSamplePeriod(Period period)
  {
    _samplePeriod = period.getPeriod();
  }

  /**
   * Returns the sample period
   */
  public long getSamplePeriod()
  {
    return _samplePeriod;
  }
  
  private boolean isEnabled()
  {
    return _healthSystem != null && _healthSystem.isEnabled();
  }

  @PostConstruct
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    if (! _isResinServer) {
      return;
    }

  }

  @PreDestroy
  public void destroy()
  {
    _statDatabase = null;
    _statService = null;
  }

  public void start()
  {
    init();
    
    if (! _lifecycle.toActive() || ! _isResinServer) {
      return;
    }
    
    Path dataDirectory = RootDirectorySystem.getCurrentDataDirectory();
    
    _healthSystem = HealthSubSystem.getCurrent();
    
    _statDatabase = new StatDatabase(dataDirectory);
    _statService = new StatServiceCluster(this);

    NetworkSystem clusterService = NetworkSystem.getCurrent();
    ServerBartender selfServer = clusterService.getSelfServer();
    _serverIndex = selfServer.getServerIndex();
    
    if (! isEnabled()) {
      return;
    }

    // the start sample inserts a single value at start time
    long now = CurrentTime.getCurrentTime();
    
    _startTimeSampleId = initSample(now, "Caucho|Uptime|Start Time", now);
    
    long startCountId = initSample(now, "Caucho|Uptime|Start Count", 1);
    addSampleValue(now + _samplePeriod, startCountId, 0);
    
    _upSampleId = initSample(now, "Caucho|Uptime|Up", UpMeterValue.START.value());
    addMeter(new ConstantStatAttribute("Caucho|Uptime|Up", 
                                       UpMeterValue.UP.value()));
    
    // JVM delta stats
    try {
      for (Object objName : queryNames("java.lang:type=GarbageCollector,*")) {
        ObjectName name = (ObjectName) objName;

        addJmxDeltaMeter("JVM|Memory|GC Time|" + name.getKeyProperty("name"),
                          name.toString(),
                          "CollectionTime");
      }
    } catch (Exception e) {
        e.printStackTrace();
        log.log(Level.FINE, e.toString(), e);
    }

    if (_jniCpuStat != null) {
      int max = _jniCpuStat.getCpuMax();
      
      for (int i = 0; i <= max; i++) {
        String name;
        double scale = 100.0;

        if (i == 0) {
          scale = 100 * max;
          if (scale == 0)
            scale = 100;
        }

        if (i == 0)
          name = "OS|CPU|CPU Active";
        else
          name = "OS|CPU|CPU Active|cpu-" + (i - 1);

        MeterBase probe = _jniCpuStat.createActiveProbe(name, i, scale);

        if (i == 0)
          _jniCpuMeter = probe;

        addMeter(probe);
        
        _cpuMeters.add(probe);

        if (i == 0)
          name = "OS|CPU|CPU User";
        else
          name = "OS|CPU|CPU User|cpu-" + (i - 1);

        addMeter(_jniCpuStat.createUserProbe(name, i, scale));

        if (i == 0)
          name = "OS|CPU|CPU System";
        else
          name = "OS|CPU|CPU System|cpu-" + (i - 1);

        addMeter(_jniCpuStat.createSystemProbe(name, i, scale));
      }

      String name = "OS|CPU|Context Switch Count";
      addMeter(_jniCpuStat.createContextSwitchProbe(name));
    }

    if (_jniNetStat != null) {
      String name = "OS|Network|tcp-established";
      int type = JniNetStat.TCP_ESTABLISHED;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-syn-sent";
      type = JniNetStat.TCP_SYN_SENT;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-syn-recv";
      type = JniNetStat.TCP_SYN_RECV;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-fin-wait1";
      type = JniNetStat.TCP_FIN_WAIT1;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-fin-wait2";
      type = JniNetStat.TCP_FIN_WAIT2;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-time-wait";
      type = JniNetStat.TCP_TIME_WAIT;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-close";
      type = JniNetStat.TCP_CLOSE;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-close-wait";
      type = JniNetStat.TCP_CLOSE_WAIT;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-last-ack";
      type = JniNetStat.TCP_LAST_ACK;
      addMeter(_jniNetStat.createActiveProbe(name, type));

      name = "OS|Network|tcp-listen";
      type = JniNetStat.TCP_LISTEN;
      addMeter(_jniNetStat.createActiveProbe(name, type));
    }

    if (_jniVmStat != null) {
      String name = "OS|Process|vm-size";
      int type = JniVmStat.VM_SIZE;
      addMeter(_jniVmStat.createActiveMeter(name, type));

      name = "OS|Process|vm-rss";
      type = JniVmStat.VM_RSS;
      addMeter(_jniVmStat.createActiveMeter(name, type));
    }
    
    for (HealthAnalyzer analyzer : _analyzers) {
      analyzer.start();
    }

    now = CurrentTime.getCurrentTime();
    sampleData(now, false);

    _alarm.queue(_samplePeriod);
  }

  public ArrayList<MeterBase> getCpuMeters()
  {
    return _cpuMeters;
  }

  public Set queryNames(String objectName)
  {
    try {
      ObjectName objName = new ObjectName(objectName);

      return _mbeanServer.queryNames(objName, null);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public void addJmxMeter(String name, String objectName, String attribute)
  {
    try {
      ObjectName objName = new ObjectName(objectName);

      addMeter(new JmxStatAttribute(name, _mbeanServer, objName, attribute));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public void addJmx(JmxItem item)
  {
    MeterService.createJmx(item.getName(),
                           item.getObjectName(),
                           item.getAttribute());
  }

  public void addJmxDelta(JmxItem item)
  {
    MeterService.createJmxDelta(item.getName(),
                                item.getObjectName(),
                                item.getAttribute());
  }

  public void addJmxDeltaMeter(String name, String objectName, String attribute)
  {
    try {
      ObjectName objName = new ObjectName(objectName);

      addMeter(new JmxDeltaStatMeter(name, _mbeanServer, objName, attribute));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public void addJmxPercentMeter(String name, String objectName, String attribute)
  {
    try {
      ObjectName objName = new ObjectName(objectName);

      addMeter(new JmxPercentStatAttribute(name, _mbeanServer, objName, attribute));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  public void addAnalyzer(HealthAnalyzer analyzer)
  {
    _analyzers.add(analyzer);
  }

  public void addMeter(MeterBase probe)
  {
    String name = probe.getName();
    
    Sample sample = new Sample(name, probe);
    addSample(sample);

    if (probe instanceof SampleMetadataAware) {
      ((SampleMetadataAware)probe).setSampleMetadata(sample.getId(), 
                                                     sample.getName());
    }
  }
  
  public MeterBase getMeter(String name)
  {
    for (Sample sample : _sampleRef.get()) {
      if (sample.getName().equals(name)) {
        return sample.getMeter();
      }
    }
    
    return null;
  }
  
  public void addSample(Sample sample)
  {
    if (! isEnabled()) {
      return;
    }
    
    Sample []samples = null;
    Sample []newSamples = null;

    do {
      samples = _sampleRef.get();

      for (Sample oldSample : samples) {
        if (oldSample.getId() == sample.getId()) {
          return;
        }
      }

      newSamples = new Sample[samples.length + 1];

      System.arraycopy(samples, 0, newSamples, 0, samples.length);
      newSamples[samples.length] = sample;

    } while (! _sampleRef.compareAndSet(samples, newSamples));

    _sampleIdRef.set(null);

    addSampleMetadata(sample.getId(), sample.getName());
  }


  private void addSampleMetadata(long id, String name)
  {
    addSampleDatabaseMetadata(id, name);

    if (_statService != null) {
      _statService.sendSampleMetadata(id, name);
    }
  }

  void addSampleDatabaseMetadata(long id, String name)
  {
    if (_statDatabase != null) {
      _statDatabase.addSampleMetadata(id, name);
    }
  }

  private long getSampleId(String name)
  {
    if (_statDatabase != null) {
      return _statDatabase.getSampleId(name);
    }
    else {
      return 0;
    }
  }

  @PreDestroy
  public void stop()
  {
    if (! _lifecycle.toDestroy())
      return;

    long now = CurrentTime.getCurrentTime();
    
    _alarm.dequeue();
    
    addSampleValue(now, _upSampleId, UpMeterValue.STOP.value());
    
    sampleData(now, false);
  }

  private void sample()
  {
    if (! _isResinServer) {
      return;
    }

    /*
    try {
      sampleCpu();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    */

    long now = CurrentTime.getCurrentTime();
    sampleData(now, true);
    
    analyze();
  }

  private void sampleData(long now, boolean isSample)
  {
    Sample []samples = _sampleRef.get();

    long []sampleIds = _sampleIdRef.get();

    if (sampleIds == null || samples.length != sampleIds.length) {
      sampleIds = new long[samples.length];

      for (int i = 0; i < samples.length; i++) {
        sampleIds[i] = samples[i].getId();
      }

      _sampleIdRef.set(sampleIds);
    }

    double []sampleData = new double[samples.length];

    for (int i = 0; i < samples.length; i++) {
      samples[i].sample();
    }

    for (int i = 0; i < samples.length; i++) {
      double value = samples[i].calculate();

      if (isSample) {
        sampleData[i] = value;
      }
    }

    addSampleValues(now, sampleIds, sampleData);
  }
  
  private long initSample(long now, String meterName, double data)
  {
    String sampleName = meterName;
    
    Sample sample = new Sample(sampleName, null);
    
    long id = sample.getId();
    addSampleMetadata(id, sample.getName());
    addSampleValue(now, id, data);
    
    return id;
  }

  public void addSampleValue(long now, long id, double data)
  {
    addSampleValues(now, new long[] { id } , new double[] { data });
  }

  public void addSampleValues(long now, long[]sampleIds, double []sampleData)
  {
    addSampleDatabase(now, sampleIds, sampleData);
    
    //StatServiceCluster statService = _statService;
    
    //if (statService != null) {
    //  statService.sendSample(now, sampleIds, sampleData);
    //}
  }

  void addSampleDatabase(long now, long[] ids, double[] data)
  {
    try {
      StatDatabase statDatabase = _statDatabase;

      if (statDatabase != null) {
        statDatabase.addSampleData(now, ids, data);
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  private void analyze()
  {
    for (HealthAnalyzer analyzer : _analyzers) {
      HealthStatus status = analyzer.analyze();
    }
  }

  public double getCpuLoad()
  {
    if (_jniCpuMeter != null) {
      return _jniCpuMeter.peek();
    }
    else {
      return 0;
    }
  }

  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      if (! isEnabled()) {
        return;
      }
      
      if (_selfServer != null && _selfServer.isUp()) {
        sample();
      }
    } finally {
      if (_lifecycle.isActive()) {
        long now = CurrentTime.getCurrentTime();
        long next = now + _samplePeriod + 100L;
        next = next - next % _samplePeriod;

        alarm.queueAt(next);
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  public StatServiceValue []getStatisticsData(String name,
                                              long beginTime,
                                              long endTime,
                                              long step)
  {
    long id = getSampleId(name);
    return getStatisticsData(id, beginTime, endTime, step);
  }

  public StatServiceValue []getStatisticsData(long id,
                                              long beginTime,
                                              long endTime,
                                              long step)
  {
    if (endTime < 0)
      endTime = Long.MAX_VALUE / 2;

    ArrayList<StatServiceValue> valueList
      = _statDatabase.sampleData(id, beginTime, endTime, step);

    if (valueList == null) {
      return null;
    }

    StatServiceValue []values = new StatServiceValue[valueList.size()];
    valueList.toArray(values);

    return values;
  }

  public double getLastValue(String name)
  {
    long id = getSampleId(name);
    return getLastValue(id);
  }

  public double getLastValue(long id)
  {
    if (id == 0)
      return 0;

    long now = CurrentTime.getCurrentTime();

    long beginTime = now - _samplePeriod * 3;
    long endTime = now + _samplePeriod * 3;

    ArrayList<StatServiceValue> valueList
      = _statDatabase.sampleData(id, beginTime, endTime, 1);

    if (valueList == null || valueList.size() == 0)
      return 0;

    return valueList.get(valueList.size() - 1).getValue();
  }

  public BaselineQueryResult getBaseline(String name, 
                                         long beginTime, 
                                         long endTime, 
                                         int minSampleSize)
  {
    long id = getSampleId(name);
    return getBaseline(id, beginTime, endTime, minSampleSize);
  }

  public BaselineQueryResult getBaseline(long id, 
                                         long beginTime, 
                                         long endTime, 
                                         int minSampleSize)
  {
    for (Baseline baseline : Baseline.values()) {
      long intervalStartTime = beginTime - baseline.offset();
      long intervalEndTime = endTime - baseline.offset();
      
      double value
        = _statDatabase.getBaselineAverage(id, intervalStartTime, intervalEndTime);
      
      if (! Double.isNaN(value)) {
        return new BaselineQueryResult(L.l(baseline.desc()), 
                                       1, value);
      }
    }
    
    return null;
  }

  public String []getStatisticsNames()
  {
    return _statDatabase.getStatisticsNames();
  }

  public void addMeterGraph(MeterGraphInfo meterGraph)
  {
    _meterGraphs.add(meterGraph);
  }

  public void addMeterGraphPage(MeterGraphPageInfo meterGraphPage)
  {
    _meterGraphPages.add(meterGraphPage);
  }

  public MeterGraphInfo []getMeterGraphs()
  {
    MeterGraphInfo []meterGraphs = new MeterGraphInfo[_meterGraphs.size()];
    
    return _meterGraphs.toArray(meterGraphs);
  }

  public MeterGraphPageInfo []getMeterGraphPages()
  {
    MeterGraphPageInfo []meterGraphPages
      = new MeterGraphPageInfo[_meterGraphPages.size()];
    
    return _meterGraphPages.toArray(meterGraphPages);
  }
  
  public MeterGraphPageInfo getMeterGraphPage(String name)
  {
    for (MeterGraphPageInfo page : _meterGraphPages) {
      if (page.getName().equals(name)) {
        return page;
      }
    }
    
    return null;
  }

  public long []getStartTimes(int index, long startTime, long endTime)
  {
    ArrayList<StatServiceValue> valueList = 
      _statDatabase.sampleData(_startTimeSampleId, startTime, endTime, 1, true);
    
    if (valueList == null || valueList.isEmpty())
      return new long[0];
    
    List<Long> times = new ArrayList<Long>();
    
    for (StatServiceValue statValue : valueList) {
      long time = Math.round(statValue.getMax());
      if (time > 0) {
        times.add(time);
      }
    }
    
    long[] startTimes = new long[times.size()];
    for (int i=0; i<startTimes.length; i++) {
      startTimes[i] = times.get(i);
    }
    
    return startTimes;
  }
  
  public DownTime []getDownTimes(int index, long beginTime, long endTime)
  {
    List<StatServiceValue> valueList = 
      _statDatabase.sampleData(_upSampleId, beginTime, endTime, 1, true);
    
    if (valueList == null || valueList.isEmpty())
      return new DownTime[0];
    
    List<DownTime> downTimes = new ArrayList<DownTime>();
    
    StatServiceValue lastUp = null;
    StatServiceValue lastStop = null;
    StatServiceValue lastStat = null;
    
    boolean isFirst = true;
      
    for (int i = 0; i < valueList.size(); i++) {
      StatServiceValue stat = valueList.get(i);
      
      if (stat.getValue() == UpMeterValue.STOP.value()) {
        lastStop = stat;
      } else if (stat.getValue() == UpMeterValue.UP.value()) {
        lastUp = stat;
      } else if (stat.getValue() == UpMeterValue.START.value()) {
        DownTime downTime = new DownTime(stat.getTime());
        
        if (lastStop != null) {
          downTime.setStartTime(lastStop.getTime());
          downTime.setEstimated(false);
        } else if (lastUp != null) {
          downTime.setStartTime(lastUp.getTime());
        } else if(lastStat != null) {
          downTime.setStartTime(lastStat.getTime());
        } else if (isFirst) {
          downTime.setStartTime(beginTime);
          downTime.setDataAbsent(true);
        }
        
        lastUp = null;
        lastStop = null;
        
        downTimes.add(downTime);
      }
      
      lastStat = stat;
      isFirst = false;
    }
    
    DownTime []array = new DownTime[downTimes.size()];
    downTimes.toArray(array);
    
    return array;
  }

  private enum Baseline {
    ONE_WEEK("Last Week", (1000 * 60 * 60 * 24 * 7)),
    ONE_DAY("Yesterday", (1000 * 60 * 60 * 24)),
    ONE_HOUR("Last Hour", (1000 * 60 * 60));
    
    private final String _desc;
    private final long _offset;
    
    Baseline(String desc, long offset) {
      _desc = desc;
      _offset = offset;
    }
    
    String desc() {
      return _desc;
    }
    
    long offset() {
      return _offset;
    }
  }
  
  public enum UpMeterValue {
    START(2),
    UP(3),
    STOP(1);
    
    private final int _value;
    
    UpMeterValue(int value)
    {
      _value = value;
    }
    
    int value()
    {
      return _value;
    }
  }  

  public class Admin
    extends ManagedObjectBase
    implements StatServiceMXBean
  {
    Admin()
    {
      registerSelf();
    }

    @Override
    public String getName()
    {
      return null;
    }

    @Override
    public String getType()
    {
      return "StatService";
    }

    @Override
    public long getSamplePeriod()
    {
      return _samplePeriod;
    }

    @Override
    public StatServiceValue []statisticsData(String name,
                                             long beginTime,
                                             long endTime,
                                             long step)
    {
      return getStatisticsData(name, beginTime, endTime, step);
    }

    @Override
    public double getLastValue(String name)
    {
      return StatServiceLocalImpl.this.getLastValue(name);
    }
    
    @Override
    public BaselineQueryResult getBaseline(String name,
                                           long beginTime,
                                           long endTime,
                                           int minSampleSize)
    {
      return StatServiceLocalImpl.this.getBaseline(name,
                                              beginTime, 
                                              endTime, 
                                              minSampleSize);
    }
    
    @Override
    public String []statisticsNames()
    {
      return StatServiceLocalImpl.this.getStatisticsNames();
    }

    @Override
    public MeterGraphInfo []getMeterGraphs()
    {
      return StatServiceLocalImpl.this.getMeterGraphs();
    }

    @Override
    public MeterGraphPageInfo []getMeterGraphPages()
    {
      return StatServiceLocalImpl.this.getMeterGraphPages();
    }

    @Override
    public MeterGraphPageInfo getMeterGraphPage(String name)
    {
      return StatServiceLocalImpl.this.getMeterGraphPage(name);
    }

    @Override
    public long []getStartTimes(int index, long beginTime, long endTime)
    {
      return StatServiceLocalImpl.this.getStartTimes(index, beginTime, endTime);
    }
    
    @Override
    public DownTime []getDownTimes(int index, long beginTime, long endTime)
    {
      return StatServiceLocalImpl.this.getDownTimes(index, beginTime, endTime);
    }
  }
}
