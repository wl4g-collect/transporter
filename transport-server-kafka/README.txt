<1> 创建Kafka主题：
./kafka-topics.sh --zookeeper safecloud-master:3181 --create --topic transporter_push_message --partitions 10 --replication-factor 1
./kafka-topics.sh --zookeeper safecloud-master:3181 --create --topic transporter_push_ack --partitions 10 --replication-factor 1
