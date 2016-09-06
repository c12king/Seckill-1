package com.seckill.web;

//import com.seckill.amqp.RabbitMQSender;

import com.seckill.dto.Exposer;
import com.seckill.dto.SeckillExcution;
import com.seckill.dto.SeckillResult;
import com.seckill.entity.Seckill;
import com.seckill.enums.SeckillStatEnum;
import com.seckill.exception.RepeatKillException;
import com.seckill.exception.SeckillCloseException;
import com.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午5:54.
 */
@RestController
@RequestMapping("/seckill") // 模块/资源/{id}/细分
public class SeckillController {

    private final static Logger LOG = LoggerFactory.getLogger(SeckillController.class);

    @Autowired
    private SeckillService seckillService;

//    @Autowired
//    private RabbitMQSender rabbitMQSender;

//    @Autowired
//    private RedisDao redisDao;

//    @RequestMapping(value = "/list", method = RequestMethod.GET)
//    public List<Seckill> list() {
//        return seckillService.getSeckillList();
//    }

    @RequestMapping(value = "/{seckillId}/detail", method = RequestMethod.GET)
    public Seckill detail(@PathVariable("seckillId") Long seckillId) {
        if (seckillId == null) {
            return null;
        }
        return seckillService.queryById(seckillId);
    }

    @RequestMapping(value = "/{seckillId}/exposer", method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId) {
        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            return new SeckillResult<Exposer>(true, exposer);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new SeckillResult<Exposer>(false, e.getMessage());
        }
    }

    @RequestMapping(value = "/{seckillId}/{md5}/excution", method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    public SeckillResult<SeckillExcution> excute(
            @PathVariable("seckillId") Long seckillId,
            @PathVariable("md5") String md5,
            @CookieValue(value = "killPhone", required = false) Long phone) {
        try {
            SeckillExcution seckillExcution = seckillService.executeSeckill(seckillId, phone, md5);
            return new SeckillResult<SeckillExcution>(true, seckillExcution);
        } catch (RepeatKillException e) {
            SeckillExcution seckillExcution = new SeckillExcution(seckillId, SeckillStatEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExcution>(false, seckillExcution);
        } catch (SeckillCloseException e) {
            SeckillExcution seckillExcution = new SeckillExcution(seckillId, SeckillStatEnum.END);
            return new SeckillResult<SeckillExcution>(false, seckillExcution);
        } catch (Exception e) {
            SeckillExcution seckillExcution = new SeckillExcution(seckillId, SeckillStatEnum.INNER_ERROR);
            return new SeckillResult<SeckillExcution>(false, seckillExcution);
        }
    }

    @RequestMapping(value = "/time/now", method = RequestMethod.GET)
    public SeckillResult<Long> time() {
        Date now = new Date();
        return new SeckillResult<Long>(true, now.getTime());
    }

//    @RequestMapping(value = "/send", method = RequestMethod.GET)
//    public Message testSender() {
//        Seckill seckill = new Seckill();
//        seckill.setName("lfx");
//        boolean result = redisDao.setxSeckill(seckill);
//        System.out.println("redisResult ---> " + result);
//        return rabbitMQSender.sendSeckill(seckill);
//    }
}
