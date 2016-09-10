package com.seckill.service;

import com.seckill.ApplicationMain;
import com.seckill.constants.WebConstants;
import com.seckill.dto.Exposer;
import com.seckill.entity.PayInfo;
import com.seckill.entity.Seckill;
import com.seckill.entity.SuccessKilled;
import com.seckill.exception.SeckillCloseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/9/2 下午7:01.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ApplicationMain.class)
@WebAppConfiguration
public class SeckillServiceTest {

    @Autowired
    private SeckillService seckillService;

    private int threadNums = 2;

    private CountDownLatch endLatch = new CountDownLatch(threadNums);

    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger failureCount = new AtomicInteger(0);

    private AtomicLong successTotalCount = new AtomicLong(0);
    private AtomicLong successTimeCount = new AtomicLong(0);
    private AtomicLong failureTimeCount = new AtomicLong(0);

    private long seckillId = 1003;

    private long userPhone = 12211111111L;

    private String md5;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, Seckill> seckillOper;


    @Test
    public void queryTest() {
        seckillService.queryById(seckillId);
    }

    @Test
    public void exposeTest() throws InterruptedException {
        //先存入一个值
        seckillOper.set(WebConstants.getSeckillRedisKey(seckillId), seckillService.queryById(seckillId));
        for (int i = 0; i < threadNums; i++) {
            new ExposeThread().start();
        }

        endLatch.await();
    }

    @Test
    public void SeckillThread() throws InterruptedException {

        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        md5 = exposer.getMd5();

        String stocks = WebConstants.getSeckillStockRedisKey(seckillId);
        //先设置库存量
        seckillOper.getOperations().delete(stocks);
        seckillOper.increment(stocks,30);

        for (int i = 0; i < threadNums; i++) {
            new SeckillThread().start();
        }

        endLatch.await();
        DecimalFormat format = new DecimalFormat("#,###");
        System.out.println("successCount : " + successCount.get());
        System.out.println("failureCount : " + failureCount.get());
        System.out.println("successTimeCount : " + format.format(successTimeCount.get()) + "ms");
        System.out.println("successTimeAvg : " + format.format(successTimeCount.get() / threadNums) + "ms");
        System.out.println("failureTimeCount : " + format.format(failureTimeCount.get()) + "ms");
        System.out.println("failureTimeAvg : " + format.format(failureTimeCount.get() / threadNums) + "ms");
    }

    @Test
    public void parsePayInfoTest() throws InterruptedException {

        //先存入一个值
        String seckillKey = WebConstants.getSeckillRedisKey(seckillId);
        seckillOper.getOperations().delete(seckillKey);
        seckillOper.set(seckillKey, seckillService.queryById(seckillId));

//        Thread.sleep(10000);

        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        md5 = exposer.getMd5();

        String stockKey = WebConstants.getSeckillStockRedisKey(seckillId);
        //先设置库存量
        seckillOper.getOperations().delete(stockKey);
        seckillOper.increment(stockKey, 30);

        for (int i = 0; i < threadNums; i++) {
            new PayThread().start();
        }

        endLatch.await();
        DecimalFormat format = new DecimalFormat("#,###");

        System.out.println("successCount : " + successCount.get());
        System.out.println("failureCount : " + failureCount.get());
        System.out.println("successTimeCount : " + format.format(successTimeCount.get()) + "ms");
        System.out.println("successTimeAvg : " + format.format(successTimeCount.get() / threadNums) + "ms");

        System.out.println("failureTimeCount : " + format.format(failureTimeCount.get()) + "ms");
        System.out.println("failureTimeAvg : " + format.format(failureTimeCount.get() / threadNums) + "ms");

        System.out.println("excutTime + payTime:Count : " + format.format(successTotalCount.get()) + "ms");
        System.out.println("excutTime + payTime:Avg : " + format.format(successTotalCount.get() / threadNums) + "ms");

        Thread.sleep(1000 * 10);
    }

    @Test
    public void executeSeckillProcTest() {
        SuccessKilled successKilled = new SuccessKilled(seckillId, userPhone, (short)2);
        seckillService.executeSeckillProc(successKilled);
    }

    class ExposeThread extends Thread {

        @Override
        public void run() {
            long nanoTime = System.currentTimeMillis();
            seckillService.exportSeckillUrl(seckillId);
            nanoTime = System.currentTimeMillis() - nanoTime;
            System.out.println("nanoTime: " + nanoTime);

            endLatch.countDown();
        }
    }

    class SeckillThread extends Thread {

        @Override
        public void run() {
            long nanoTime = System.currentTimeMillis();
            try {
                seckillService.executeSeckill(seckillId, userPhone, md5);
                nanoTime = System.currentTimeMillis() - nanoTime;
                System.out.println("millisTime: " + nanoTime);

                successCount.incrementAndGet();
                successTimeCount.addAndGet(nanoTime);
            } catch (SeckillCloseException e) {
                nanoTime = System.currentTimeMillis() - nanoTime;
                System.out.println("millisTime: " + nanoTime);
                failureCount.incrementAndGet();
                failureTimeCount.addAndGet(nanoTime);
            } finally {
                endLatch.countDown();
            }
        }
    }

    class PayThread extends Thread {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                seckillService.executeSeckill(seckillId, userPhone, md5);
                long excutTime = System.currentTimeMillis() - startTime;
                System.out.println("excutTime: " + excutTime);

                successCount.incrementAndGet();
                successTimeCount.addAndGet(excutTime);

                PayInfo payInfo = new PayInfo(seckillId, "TRADE_SUCCESS");
                seckillService.parsePayInfo(payInfo, seckillId, userPhone);
                long totalTime = System.currentTimeMillis() - startTime;
                System.out.println("excutTime + payTime: " + totalTime);
                successTotalCount.addAndGet(totalTime);

            } catch (SeckillCloseException e) {
                long excutTime = System.currentTimeMillis() - startTime;
                System.out.println("excutTime: " + excutTime);
                failureCount.incrementAndGet();
                failureTimeCount.addAndGet(excutTime);
            } finally {
                endLatch.countDown();
            }
        }
    }

}
