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
        String redisServer = ip + ":" + port;

        JedisPoolConfig poolConfig = new JedisPoolConfig();

        //List<JedisShardInfo> shards = Arrays.asList(new JedisShardInfo(ip, port));
        //ShardedJedisPool shardedJedisPool = new ShardedJedisPool(poolConfig, shards);
        //shardedJedis = shardedJedisPool.getResource();


        try {
            jedisPool = new JedisPool(new JedisPoolConfig(), redisServer);

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

        // 从zookeeper读取redis配置信息

        // 生产6位随机数
        String captcha = "";
        captcha = getRandom();


        // 存储

        Jedis jedis = jedisPool.getResource();
        if (jedis == null)
            return false;

        try {
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

        // 存储
        Jedis jedis = jedisPool.getResource();
        if (jedis == null)
            return false;

        try {
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

    public static void main(String[] args)
    {
        SMSCaptcha test = new SMSCaptcha();
        System.out.println(test.getRandom());
    }
}
