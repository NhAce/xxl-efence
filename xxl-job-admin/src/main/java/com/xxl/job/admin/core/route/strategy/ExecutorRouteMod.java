package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 用任务id对执行器数量进行取模运算，保证所有任务平均分配到所有执行器上，
 */
public class ExecutorRouteMod extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        try {
            int mod = triggerParam.getJobId() % addressList.size();
            return new ReturnT<>(addressList.get(mod));
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e +"");
        }
    }
}
