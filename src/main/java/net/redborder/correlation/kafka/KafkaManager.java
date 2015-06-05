package net.redborder.correlation.kafka;

import java.util.*;

public class KafkaManager {
    private static ConsumerManager consumerManager;
    private static List<Topic> topics;

    public static void init() {
        initTopics();
        consumerManager = new ConsumerManager();
        consumerManager.start(KafkaManager.getTopics());
    }

    private static void initTopics(){
        // TODO initiate kafka topics with partitions.
    }

    public static List<Topic> getTopics(){
        return topics;
    }

    private static void shutdown(){
        consumerManager.shutdown();
    }
}