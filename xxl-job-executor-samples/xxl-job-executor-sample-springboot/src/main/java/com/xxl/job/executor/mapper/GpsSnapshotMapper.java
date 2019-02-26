package com.xxl.job.executor.mapper;

import com.xxl.job.executor.entity.GpsSnapshot;
import java.util.List;

public interface GpsSnapshotMapper {
    int deleteByPrimaryKey(String deviceCode);

    int insert(GpsSnapshot record);

    GpsSnapshot selectByPrimaryKey(String deviceCode);

    List<GpsSnapshot> selectAll();

    int updateByPrimaryKey(GpsSnapshot record);

    List<GpsSnapshot> getSnapshotsByList(List<String> deviceCodes);
}