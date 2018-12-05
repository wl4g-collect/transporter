<1> 创建Hbase表：
-- drop table if exists "transporter"."tb_push_message";
create table "transporter"."tb_push_message"("ROW" varchar primary key,"info"."classifier" varchar(50),"info"."recDate" varchar(20),"info"."srcDevice" varchar(30),"info"."dstDevice" varchar(30),"info"."payload" varchar);
