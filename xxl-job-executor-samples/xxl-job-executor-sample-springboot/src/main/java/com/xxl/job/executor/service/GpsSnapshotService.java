package com.xxl.job.executor.service;

import com.xxl.job.executor.entity.GpsSnapshot;

import java.util.List;

/**
 * Created by Administrator on 2018/11/19 0019.
 */
public interface GpsSnapshotService {
    List<GpsSnapshot> getSnapshotsByList(List<String> deviceCodes) throws Exception;

    GpsSnapshot getSnapshotByDeviceCode(String deviceCode) throws Exception;
}
