package com.xxl.job.executor.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Administrator on 2018/11/12 0012.
 */
@Configuration
public class RabbitMqConfig {
    @Value("${mq_host}")
    private String host;

    @Value("${mq_username}")
    private String username;

    @Value("${mq_password}")
    private String password;

    @Value("${queue_name}")
    private String queueName;

    @Value("${channel_num}")
    private int channelNum;

    @Bean
    public Producer initMQ(){
        try {
            Producer producer = new Producer(host, username, password);
            producer.initProducer(queueName, channelNum);
            return producer;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
