package com.xxl.job.executor.service.impl;

import com.xxl.job.executor.entity.GpsSnapshot;
import com.xxl.job.executor.mapper.GpsSnapshotMapper;
import com.xxl.job.executor.service.GpsSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Administrator on 2018/11/19 0019.
 */
@Service
public class GpsSnapshotServiceImpl implements GpsSnapshotService {

    @Autowired
    private GpsSnapshotMapper snapshotMapper;

    @Override
    public List<GpsSnapshot> getSnapshotsByList(List<String> deviceCodes) throws Exception {
        return snapshotMapper.getSnapshotsByList(deviceCodes);
    }

    @Override
    public GpsSnapshot getSnapshotByDeviceCode(String deviceCode) throws Exception {
        return snapshotMapper.selectByPrimaryKey(deviceCode);
    }
}
