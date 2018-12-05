
压测脚本：

1. 压测启动多个 transport-client-sdk进程脚本：
参数说明：arg0:服务器地址/arg1:并发线程数/arg2:每个线程发送消息数

生产环境：
for((i=1;i<=10;i++)) do nohup java -jar /root/transport-client-sdk-1.1.2.Final.jar 10.161.166.68:10030 100 1000 >/root/test.out 2>&1 & done

测试环境：
for((i=1;i<=10;i++)) do nohup java -jar /root/transport-client-sdk-1.1.2.Final.jar 115.29.212.28:10030 100 1000 >/root/test.out 2>&1 & done


2. Kill掉所有压测 transport-client-sdk 进程脚本：
ps -ef|grep transport-client-sdk |grep -v grep |cut -c 9-15|xargs kill -9