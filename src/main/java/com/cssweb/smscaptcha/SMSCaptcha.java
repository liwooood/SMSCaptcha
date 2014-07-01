package com.cssweb.smscaptcha;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by chenhf on 2014/7/1.
 */
public class SMSCaptcha {
    private static SMSCaptcha instance = null;


    //private ShardedJedis shardedJedis = null;


    private JedisPool jedisPool = null;

    private SMSCaptcha()
    {
    }

    public static synchronized SMSCaptcha getInstance()
    {
        if (instance == null)
            instance = new SMSCaptcha();

        return instance;
    }

    public boolean connect(String ip, int port)
    {


        JedisPoolConfig poolConfig = new JedisPoolConfig();

        //List<JedisShardInfo> shards = Arrays.asList(new JedisShardInfo(ip, port));
        //ShardedJedisPool shardedJedisPool = new ShardedJedisPool(poolConfig, shards);
        //shardedJedis = shardedJedisPool.getResource();


        try {
            // 从zookeeper读取配置信息

            jedisPool = new JedisPool(new JedisPoolConfig(), ip, port);

            if (jedisPool == null)
            {
                return false;
            }

        }
        catch(Exception exp)
        {
            return false;
        }

        return true;
    }

    public void close()
    {
        if (jedisPool != null)
        {
            jedisPool.destroy();
            jedisPool = null;
        }
    }

    /**
     *
     * @param mobile
     * @param expireSeconds
     * @return
     */
    public boolean sendCaptcha(String mobile, int expireSeconds)
    {
        boolean ret = false;

       if (jedisPool == null)
           return ret;

        // 生产6位随机数
        String captcha = "";
        captcha = getRandom();


        // 存储

        Jedis jedis = null;


        try {
            jedis = jedisPool.getResource();
            if (jedis == null)
                return false;

            Long result = jedis.hset(mobile, captcha, "");
            jedis.expire(mobile, expireSeconds);

            System.out.println("result = " + result);

            // 调用短信发送API

            ret = true;
        }
        catch(JedisConnectionException exp)
        {
            ret = false;

            if (jedis != null) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
        }
        finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
                jedis = null;
            }
        }




        return ret;
    }

    /**
     *
     * @param mobile
     * @param captcha
     * @return
     */
    public boolean verifyCaptcha(String mobile, String captcha)
    {
        boolean ret = false;

        if (jedisPool == null)
            return ret;

        // 存储
        Jedis jedis = null;

        try {
            jedis = jedisPool.getResource();
            if (jedis == null)
                return false;

            if (jedis.hexists(mobile, captcha)) {
                // 存在
                jedis.hdel(mobile, captcha);

                ret = true;
            }
            else
            {
                ret = false;
            }
        }
        catch(JedisConnectionException exp)
        {
            ret = false;

            if (jedis != null) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
        }
        finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
                jedis = null;
            }
        }

        return ret;
    }

    private String getRandom()
    {
        // 可以用java.util.Random
        // 可以用Math.Random

        String result = "";

        Random random = new Random();
        for (int i=0; i<6; i++) {
            result += random.nextInt(10);
        }

        return result;
    }

    private void getCaptcha(String mobile)
    {
        if (jedisPool == null)
            return;

        // 存储
        Jedis jedis = null;

        try {
            jedis = jedisPool.getResource();
            if (jedis == null)
                return;
/*
            //if (jedis.hget(mobile, )) {

            }
            else
            {

            }
            */
        }
        catch(JedisConnectionException exp)
        {

            if (jedis != null) {
                jedisPool.returnBrokenResource(jedis);
                jedis = null;
            }
        }
        finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
                jedis = null;
            }
        }
    }

    public static void main(String[] args)
    {
        int expireSeconds = 60;
        boolean ret = false;
        String mobile = "13918713802";
        String captcha = "";

        SMSCaptcha smsCaptcha = SMSCaptcha.getInstance();

        ret = smsCaptcha.connect("192.168.1.201", 6379);
        if (ret)
        {
            System.out.println("连接redis成功");
        }
        else
        {
            System.out.println("连接redis失败");
            return;

        }

        ret = smsCaptcha.sendCaptcha(mobile, expireSeconds);
        if (ret)
        {
            System.out.println("发送短信验证码成功");
        }

        smsCaptcha.verifyCaptcha(mobile, captcha);

        smsCaptcha.close();
    }
}
