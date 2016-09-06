-- 数据库初始化脚本

-- 创建数据库
CREATE DATABASE seckill;

USE seckill;
-- 创建秒杀库存表
CREATE TABLE seckill (
  seckill_id  BIGINT       NOT NULL AUTO_INCREMENT
  COMMENT '商品库存ID',
  name        VARCHAR(120) NOT NULL
  COMMENT '商品名称',
  number      INT          NOT NULL
  COMMENT '库存数量',
  start_time  TIMESTAMP    NOT NULL
  COMMENT '秒杀开启时间',
  end_time    TIMESTAMP    NOT NULL
  COMMENT '秒杀结束时间',
  create_time TIMESTAMP    NOT NULL
  COMMENT '创建时间',
  PRIMARY KEY (seckill_id),
  KEY idx_start_time(start_time),
  KEY idx_end_time(end_time),
  KEY idx_create_time(create_time)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 1000
  DEFAULT CHARSET = UTF8
  COMMENT = '秒杀库存表';

-- 初始化数据
INSERT INTO seckill (name, number, start_time, end_time, create_time)
VALUES
  ('1000元秒杀iphone6s', 1000, '2015-08-01 00:00:00', '2017-08-08 00:00:00', '2016-07-01 00:00:00'),
  ('500元秒杀ipad air2', 200, '2015-08-01 00:00:00', '2017-08-08 00:00:00', '2016-07-01 00:00:00'),
  ('300元秒杀小米 4s', 300, '2015-08-01 00:00:00', '2017-08-08 00:00:00', '2016-07-01 00:00:00'),
  ('100元秒杀红米', 400, '2015-08-01 00:00:00', '2017-08-08 00:00:00', '2016-07-01 00:00:00');

-- 秒杀成功明细表
CREATE TABLE success_killed (
  seckill_id  BIGINT    NOT NULL
  COMMENT '秒杀商品ID',
  user_phone  BIGINT    NOT NULL
  COMMENT '用户手机号',
  state       TINYINT   NOT NULL DEFAULT -1
  COMMENT '状态标识：-1：表示无效，0:表示成功',
  create_time TIMESTAMP NOT NULL
  COMMENT '创建时间',
  -- 基于唯一性可防护重复性秒杀
  PRIMARY KEY (seckill_id, user_phone),
  KEY idx_create_time(create_time)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = UTF8
  COMMENT = '秒杀成功明细表';

-- 秒杀减库存：返回结果 result=2：成功,result=0:未执行sql,result<0:sql有错误
CREATE PROCEDURE `execute_seckill`(IN v_seckill_id bigint, IN v_phone bigint,
  IN v_kill_time timestamp,IN v_pay_stat tinyint, OUT r_result int)
   BEGIN
    DECLARE insert_count int DEFAULT 0;
    START TRANSACTION;
    insert ignore into seckill.success_killed
    (seckill_id,user_phone,state,create_time)
    values(v_seckill_id,v_phone,v_pay_stat,v_kill_time);
    SELECT ROW_COUNT() INTO insert_count;
    IF(insert_count = 0) THEN
      rollback;
      set r_result = -1;
    ELSEIF(insert_count < 0) THEN
      rollback;
      set r_result = -2;
    -- 添加成功
    ELSE
      update seckill set number = number -1
      where seckill_id = v_seckill_id
            and end_time > v_kill_time
            and start_time < v_kill_time
            and number > 0;
      SELECT ROW_COUNT() INTO insert_count;
      IF(insert_count = 0) THEN
        rollback;
        set r_result = -3;
      ELSEIF (insert_count < 0) THEN
        rollback;
        set r_result = -4;
      -- 更新成功
      ELSE
        commit;
        set r_result = 2;
      END IF;
    END IF;
  END