/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kis.analyzenb;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

/**
 *
 * @author naoki
 */
public class JmxInflux {
    public static void main(String[] args) throws MalformedURLException, IOException, InterruptedException {
        JMXServiceURL target = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://localhost:9012/jmxrmi");
        JMXConnector connect = JMXConnectorFactory.connect(target);
        MBeanServerConnection mbeanConn = connect.getMBeanServerConnection();
        MemoryMXBean memoryBean = ManagementFactory.newPlatformMXBeanProxy(
                mbeanConn, ManagementFactory.MEMORY_MXBEAN_NAME, 
                MemoryMXBean.class);
        ThreadMXBean threadBean = ManagementFactory.newPlatformMXBeanProxy(
                mbeanConn, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);

        InfluxDB influx = InfluxDBFactory.connect("http://localhost:8086", "root", "root");
        influx.createDatabase("nblog");
        
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) 
                Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            int count = threadBean.getThreadCount();
            //System.out.printf("%s write heap:%d thread:%d%n", LocalDateTime.now(), used, count);
            BatchPoints batch = BatchPoints.database("nblog")
                    .tag("async", "true")
                    .retentionPolicy("autogen")
                    .consistency(InfluxDB.ConsistencyLevel.ALL)
                    .build();
            long now = System.currentTimeMillis();
            Point heap = Point.measurement("memory")
                    .time(now, TimeUnit.MILLISECONDS)
                    .addField("heap", used)
                    .build();
            Point thread = Point.measurement("thread")
                    .time(now, TimeUnit.MILLISECONDS)
                    .addField("count", count)
                    .build();
            batch.point(heap).point(thread);
            influx.write(batch);
        }, 0, 1, TimeUnit.SECONDS);

        
    }
}
