/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kis.analyzenb;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

/**
 *
 * @author naoki
 */
public class UsageData {
    public static void main(String[] args) {
        InfluxDB influx = InfluxDBFactory.connect("http://192.168.56.103:8086", "root", "root");
        Query q = new Query("SELECT heap FROM memory where time > now() - 120m and time < now() - 40m ", "nblog");
        QueryResult result = influx.query(q);
        TreeMap<ZonedDateTime, Double> memories = result.getResults().stream().flatMap(rs -> rs.getSeries().stream())
                .flatMap(s -> s.getValues().stream())
                .collect(Collectors.toMap(v -> ZonedDateTime.parse(v.get(0).toString(), DateTimeFormatter.ISO_DATE_TIME),
                        v -> (Double)v.get(1), (a, b) -> a, () -> new TreeMap<>()));
        
        Query q2 = new Query("SELECT count FROM thread where time > now() - 121m", "nblog");
        QueryResult result2 = influx.query(q2);
        TreeMap<ZonedDateTime, Double> threads = result2.getResults().stream().flatMap(rs -> rs.getSeries().stream())
                .flatMap(s -> s.getValues().stream())
                .collect(Collectors.toMap(v -> ZonedDateTime.parse(v.get(0).toString(), DateTimeFormatter.ISO_DATE_TIME),
                        v -> (Double)v.get(1), (a, b) -> a, () -> new TreeMap<>()));
        
        ArrayList<Map.Entry<ZonedDateTime, Double>> entries = new ArrayList<>(memories.entrySet());
        
        Map<ZonedDateTime, List<List<Double>>> heaps = IntStream.range(0, memories.size() - 512)
                .mapToObj(idx -> new SimpleEntry<>(entries.get(idx).getKey(),
                        wavletAnalysis(entries.subList(idx, idx + 512).stream()
                                .map(ent -> ent.getValue()).collect(Collectors.toList()))))
                .collect(Collectors.toMap(ent -> ent.getKey(), ent -> ent.getValue()));
        
        BackPropergation bp = new BackPropergation(10, 5);
        heaps.forEach((time, wv) -> {
            Double th = threads.get(time);
            System.out.println(th + ":" + (th > 32));
            bp.learn(th > 33 ? 1 : 0, wv.stream().mapToDouble(f -> f.get(0)).toArray());
        });


        Query q3 = new Query("SELECT heap FROM memory where time > now() - 120m ", "nblog");
        QueryResult result3 = influx.query(q3);
        TreeMap<ZonedDateTime, Double> m3 = result3.getResults().stream().flatMap(rs -> rs.getSeries().stream())
                .flatMap(s -> s.getValues().stream())
                .collect(Collectors.toMap(v -> ZonedDateTime.parse(v.get(0).toString(), DateTimeFormatter.ISO_DATE_TIME),
                        v -> (Double)v.get(1), (a, b) -> a, () -> new TreeMap<>()));
        ArrayList<Map.Entry<ZonedDateTime, Double>> e3 = new ArrayList<>(m3.entrySet());
        Map<ZonedDateTime, List<List<Double>>> heaps2 = IntStream.range(0, m3.size() - 512)
                .mapToObj(idx -> new SimpleEntry<>(e3.get(idx).getKey(),
                        wavletAnalysis(e3.subList(idx, idx + 512).stream()
                                .map(ent -> ent.getValue()).collect(Collectors.toList()))))
                .collect(Collectors.toMap(ent -> ent.getKey(), ent -> ent.getValue()));

        
        heaps2.forEach((time, wv) -> {
            BatchPoints batch = BatchPoints.database("nblog")
                    .tag("async", "true")
                    .retentionPolicy("autogen")
                    .consistency(InfluxDB.ConsistencyLevel.ALL)
                    .build();

            Point working = Point.measurement("analyse")
                    .time(time.toEpochSecond(), TimeUnit.SECONDS)
                    .addField("working", bp.trial(wv.stream().mapToDouble(f -> f.get(0)).toArray()))
                    .build();
            batch.point(working);
            influx.write(batch);
        });
    }
    
    static List<List<Double>> wavletAnalysis(List<Double> data) {
        // 離散ウェーブレット変換
        List<List<Double>> wavlets = new ArrayList<>();
        while(data.size() > 1) {
            List<Double> wavlet = new ArrayList<>();
            List<Double> next = new ArrayList<>();
            for (int i = 0; i < data.size(); i += 2) {
                wavlet.add((data.get(i) - data.get(i + 1)) / 2); // 移動差分がwavlet値
                next.add((data.get(i) + data.get(i + 1)) / 2); // 移動平均をつぎにまわす
            }
            wavlets.add(wavlet);
            data = next;
        }
        wavlets.add(data);
        return wavlets;
    }
}
