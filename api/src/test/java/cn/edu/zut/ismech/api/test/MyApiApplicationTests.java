package cn.edu.zut.ismech.api.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;

@SpringBootTest
public class MyApiApplicationTests {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Test
    public void redis(){
        //redisTemplate.opsForList().rightPush("test1","aa");
        Jedis jedis=new Jedis("localhost", 6379);
        //jedis.spop("img",1);
        //list->string
        String join = String.join(",", jedis.spop("img",1));
        System.out.println(join);
    }
    @Test
    public void testGetFromMysql(){

    }

    //redis
    @Test
    public void testRedisSet(){

        redisTemplate.opsForValue().set("hi","tom");
//        stringRedisTemplate.opsForValue().append("hi","tom");
    }
    @Test
    public void testRedisGet(){


    }
}
