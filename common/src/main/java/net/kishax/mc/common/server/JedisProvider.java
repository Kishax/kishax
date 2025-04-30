package net.kishax.mc.common.server;

import com.google.inject.Provider;

import redis.clients.jedis.Jedis;

public class JedisProvider implements Provider<Jedis> {
  @Override
  public Jedis get() {
    return new Jedis("localhost");
  }
}
