package com.seckill.service;

import com.seckill.ApplicationMain;
import com.seckill.dto.Exposer;
import com.seckill.entity.PayInfo;
import com.seckill.entity.Seckill;
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

    private long userPhone = 12111111111L;

    private String md5;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, Seckill> seckillOper;

    private static final String SECKILL_PREFIX = "seckill_";

    @Test
    public void queryTest() {
        seckillService.queryById(seckillId);
    }

    @Test
    public void exposeTest() throws InterruptedException {
        //先存入一个值
        seckillOper.set(SECKILL_PREFIX + seckillId, seckillService.queryById(seckillId));
        for (int i = 0; i < threadNums; i++) {
            new ExposeThread().start();
        }

        endLatch.await();
    }

    @Test
    public void SeckillThread() throws InterruptedException {

        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        md5 = exposer.getMd5();

        String stocks = seckillId + "_stocks";
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
        seckillOper.getOperations().delete(SECKILL_PREFIX + seckillId);
        seckillOper.set(SECKILL_PREFIX + seckillId, seckillService.queryById(seckillId));
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        md5 = exposer.getMd5();

        String stocks = seckillId + "_stocks";
        //先设置库存量
        seckillOper.getOperations().delete(stocks);
        seckillOper.increment(stocks, 30);

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
    }

    @Test
    public void executeSeckillProcTest() {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("seckillId", 1003L);
        paramMap.put("userPhone", 12345678903L);
        paramMap.put("killTime", new Date());
        paramMap.put("payStat",2);
        seckillService.executeSeckillProc(paramMap);
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