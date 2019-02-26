package com.xxl.job.executor.service.jobhandler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.executor.common.Const;
import com.xxl.job.executor.core.config.Producer;
import com.xxl.job.executor.entity.GpsSnapshot;
import com.xxl.job.executor.service.GpsSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/11/13 0013.
 */
@JobHandler(value = "efenceJobHandler")
@Component
public class EfenceJobHandler extends IJobHandler {
    private static String IN_EFENCE = "在围栏内";
    private static String OUT_EFENCE = "在围栏外";
    private static String BROKER = "------------";
    private Map<String, Boolean> deviceStatus = new HashMap<>();

    @Autowired
    private Producer producer;

    @Autowired
    private GpsSnapshotService gpsSnapshotService;

    @Override
    public ReturnT<String> execute(String param) throws Exception {
        try {
            long startTime = System.currentTimeMillis();

            JSONObject paramObject = (JSONObject) JSONObject.parse(param);
            //设备号
            String deviceCodes = paramObject.getString(Const.DEVICE_CODES);
            List<String> deviceCodeList = JSONObject.parseArray(deviceCodes, String.class);
            if (deviceStatus.size() == 0) {
                deviceStatus = new HashMap<>(deviceCodeList.size());
            }
            //电子围栏类型 1：多边形 2：圆形
            int efenceType = paramObject.getIntValue(Const.EFENCE_TYPE);
            //报警类型 1：进栏报警 2：出栏报警 3：进出栏报警
            int alarmType = paramObject.getIntValue(Const.ALARM_TYPE);
            //电子围栏坐标
            String coordinates = paramObject.getString(Const.COORDINATES);
            if (StringUtils.isEmpty(deviceCodes) || StringUtils.isEmpty(coordinates)) {
                XxlJobLogger.log("设备号或围栏坐标为空");
                return null;
            }
            long time1 = System.currentTimeMillis();
            List<GpsSnapshot> gpsSnapshots = gpsSnapshotService.getSnapshotsByList(deviceCodeList);
            System.out.println("select cost: " + (System.currentTimeMillis() / 1000 - time1 / 1000));
            for (GpsSnapshot snapshot : gpsSnapshots) {
//            GpsSnapshot snapshot = gpsSnapshotService.getSnapshotByDeviceCode(deviceCode);
                if (snapshot != null) {
                    double lon = snapshot.getLon() == null?0:snapshot.getLon();
                    double lat = snapshot.getLat() == null?0:snapshot.getLat();
                    String deviceCode = snapshot.getDeviceCode();
                    boolean inEfence = false;
                    if (efenceType == 1) {//电子围栏是多边形
                        inEfence = pointInPolygon(lon / 1000000, lat / 1000000, coordinates);
                    } else if (efenceType == 2) {//电子围栏是圆形
                        inEfence = pointInCircle(lon / 1000000, lat / 1000000, coordinates);
                    }
                    if (deviceCode.equals("018040008128") || deviceCode.equals("191817069127")) {
                        XxlJobLogger.log(deviceCode + BROKER + (inEfence ? IN_EFENCE : OUT_EFENCE));
                    }
                    long time = System.currentTimeMillis() / 1000;
                    if (deviceStatus.containsKey(deviceCode)) {
                        boolean status = deviceStatus.get(deviceCode);
                        if (status != inEfence) {
                            deviceStatus.put(deviceCode, inEfence);
                            StringBuilder alarmInfo = new StringBuilder();
                            if (alarmType == 1 && inEfence) {//报警类型是进栏报警，且设备上次在围栏外，这次在围栏内，组装进栏报警信息
                                alarmInfo.append(deviceCode).append(Const.COMMA).append(1).append(Const.COMMA).append(time);
                            } else if (alarmType == 2 && !inEfence) {//报警类型是出栏报警，且设备上次在围栏内，这次在围栏外，组装出栏报警信息
                                alarmInfo.append(deviceCode).append(Const.COMMA).append(2).append(Const.COMMA).append(time);
                            } else if (alarmType == 3) {//报警类型是进出栏报警，根据设备这次在围栏内部或外部，组装对应的报警类型
                                if (inEfence) {
                                    alarmInfo.append(deviceCode).append(Const.COMMA).append(1).append(Const.COMMA).append(time);
                                } else {
                                    alarmInfo.append(deviceCode).append(Const.COMMA).append(2).append(Const.COMMA).append(time);
                                }
                            }
                            //将组装好的报警信息发送到 mq 中
                            producer.sendMessage(alarmInfo.toString());
                        }
                    } else {
                        deviceStatus.put(deviceCode, inEfence);
                    }
                } else {
                    XxlJobLogger.log("redis 为空");
                }
            }
            //从redis中获取对应设备的最新经纬度信息
//        Jedis jedis = jedisPool.getResource();
//        Pipeline pipeline = jedis.pipelined();
//        for(String deviceCode : deviceCodeList){
//            pipeline.get(deviceCode);
//        }
//        List<Object> objectList = pipeline.syncAndReturnAll();
//        //遍历集合，处理每个设备
//            for (Object object : objectList) {
//                if (object != null) {
//                    JSONObject snapshot = (JSONObject) JSONObject.parse(object.toString());
//                    String deviceCode = snapshot.getString(Const.DEVICE_CODE);
//                    BigDecimal lonBig = (BigDecimal) snapshot.get(Const.LON);
//                    BigDecimal latBig = (BigDecimal) snapshot.get(Const.LAT);
//                    double lon = lonBig.doubleValue();
//                    double lat = latBig.doubleValue();
//                    boolean inEfence = false;
//                    if (efenceType == 1) {//电子围栏是多边形
//                        inEfence = pointInPolygon(lon, lat, coordinates);
//                    } else if (efenceType == 2) {//电子围栏是圆形
//                        inEfence = pointInCircle(lon, lat, coordinates);
//                    }
//                    long time = System.currentTimeMillis() / 1000;
//                    if (deviceStatus.containsKey(deviceCode)) {
//                        boolean status = deviceStatus.get(deviceCode);
//                        if (status != inEfence) {
//                            deviceStatus.put(deviceCode, inEfence);
//                            StringBuilder alarmInfo = new StringBuilder();
//                            if (alarmType == 1 && inEfence) {//报警类型是进栏报警，且设备上次在围栏外，这次在围栏内，组装进栏报警信息
//                                alarmInfo.append(deviceCode).append(Const.COMMA).append(1).append(Const.COMMA).append(time);
//                            } else if (alarmType == 2 && !inEfence) {//报警类型是出栏报警，且设备上次在围栏内，这次在围栏外，组装出栏报警信息
//                                alarmInfo.append(deviceCode).append(Const.COMMA).append(2).append(Const.COMMA).append(time);
//                            } else if (alarmType == 3) {//报警类型是进出栏报警，根据设备这次在围栏内部或外部，组装对应的报警类型
//                                if (inEfence) {
//                                    alarmInfo.append(deviceCode).append(Const.COMMA).append(1).append(Const.COMMA).append(time);
//                                } else {
//                                    alarmInfo.append(deviceCode).append(Const.COMMA).append(2).append(Const.COMMA).append(time);
//                                }
//                            }
//                            //将组装好的报警信息发送到 mq 中
//                            producer.sendMessage(alarmInfo.toString());
//                        }
//                    } else {
//                        deviceStatus.put(deviceCode, inEfence);
//                    }
//                }else {
//                    XxlJobLogger.log("redis 为空");
//                }
//            }
            long endTime = System.currentTimeMillis();
            System.out.println("time cost: " + (endTime / 1000 - startTime / 1000));
        }catch (Exception e){
            e.printStackTrace();
            XxlJobLogger.log(e);
        }
        return SUCCESS;
    }

    /**
     * 根据经纬度判断坐标是否在多边形电子围栏内
     * @param lon 经度
     * @param lat 纬度
     * @param coordinates 围栏坐标点
     * @return
     */
    private boolean pointInPolygon(double lon, double lat, String coordinates){
        JSONArray jsonArray = JSONArray.parseArray(coordinates);
        int polyCorners = jsonArray.size();
        Double[] polyLon = new Double[polyCorners];
        Double[] polyLat = new Double[polyCorners];
        int i = 0;
        int j = polyCorners - 1;
        boolean oddNodes = false;
        for (int m = 0; m < jsonArray.size(); m++){
            String pointStr = JSONArray.toJSONString(jsonArray.get(m));
            JSONArray pointArray = JSONArray.parseArray(pointStr);
            polyLon[m] = Double.valueOf(pointArray.get(0).toString());
            polyLat[m] = Double.valueOf(pointArray.get(1).toString());
        }
        for (i = 0; i < polyCorners; i++){
            if ((polyLat[i] < lat && polyLat[j] >= lat || polyLat[j] < lat && polyLat[i] >= lat) &&  (polyLon[i]<=lon || polyLon[j]<=lon)){
                if (polyLon[i]+(lat-polyLat[i])/(polyLat[j]-polyLat[i])*(polyLon[j]-polyLon[i])<=lon){
                    oddNodes = !oddNodes;
                }
            }
            j = i;
        }
        return oddNodes;
    }

    /**
     * 判断坐标点是否在圆形电子围栏内
     * @param lon 经度
     * @param lat 纬度
     * @param coordinates 围栏坐标点
     * @return
     */
    private boolean pointInCircle(double lon, double lat, String coordinates){
        double centerLon = Double.valueOf(JSONArray.parseArray(JSONArray.parseArray(coordinates).get(0).toString()).get(0).toString());
        double centerLat = Double.valueOf(JSONArray.parseArray(JSONArray.parseArray(coordinates).get(0).toString()).get(1).toString());
        double edgeLon = Double.valueOf(JSONArray.parseArray(JSONArray.parseArray(coordinates).get(1).toString()).get(0).toString());
        double edgeLat = Double.valueOf(JSONArray.parseArray(JSONArray.parseArray(coordinates).get(1).toString()).get(1).toString());
        return (Math.pow(lon - centerLon, 2) + Math.pow(lat - centerLat, 2)) <=
                (Math.pow(edgeLon - centerLon, 2) + Math.pow(edgeLat - centerLat, 2));
    }
}
