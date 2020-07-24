//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.javamaster.idea.redis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.javamaster.idea.redis.model.RedisInfo;
import redis.clients.jedis.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author yudong
 * @date 2020/7/23
 */
public class RedisUtils {
    public static final String COMMA = ",";
    public static final String OS_WINDOWS = "Windows";
    private static final String REDIS_CONFIGS = "redis-cli:configs:redis-lists";
    public static ObjectMapper objectMapper = new ObjectMapper();
    public static Observable observable = new Observable() {
        @Override
        public void notifyObservers() {
            setChanged();
            super.notifyObservers();
        }
    };
    private static Jedis jedis;
    private static JedisCluster jedisCluster;
    private static List<Jedis> jedisClusterList;
    private static JedisCommands jedisCommands;
    private static boolean useSingle;
    private static String winCommandline;
    private static String linuxCommandline;
    private static boolean initialize;

    public static boolean notInitialize() {
        return !initialize;
    }

    public static List<RedisInfo> getRedisInfos() {
        return Collections.unmodifiableList(new ArrayList<>(getDefRedisInfo()));
    }

    public static void initCommandLine(String ip, int port, String password) {
        winCommandline = " -h " + ip + " -p " + port + (StringUtil.isEmpty(password) ? "" : " -a " + password);
        linuxCommandline = "redis-cli -h " + ip + " -p " + port + (StringUtil.isEmpty(password) ? "" : " -a " + password);
    }


    public static void setDefaultServer(int index) {
        List<RedisInfo> redisInfos = getDefRedisInfo();
        redisInfos.get(index).setDef(true);
        for (int j = 0; j < redisInfos.size(); j++) {
            if (j != index) {
                redisInfos.get(j).setDef(false);
            }
        }
        persistentServers(redisInfos);
        observable.notifyObservers();
    }

    public static void delServer(int index) {
        List<RedisInfo> redisInfos = getDefRedisInfo();
        redisInfos.remove(index);
        persistentServers(redisInfos);
        observable.notifyObservers();
    }

    public static void addServer(String serverInfo) {
        if (serverInfo == null) {
            return;
        }
        List<RedisInfo> redisInfos = getDefRedisInfo();
        String[] strings = serverInfo.split("\\|");
        if (!serverInfo.contains(COMMA)) {
            RedisInfo redisInfo = new RedisInfo(strings[0].split(":")[0], strings[0].split(":")[1], strings[1]);
            redisInfos.add(redisInfo);
        } else {
            String[] urls = strings[0].split(",");
            String password = strings[1];
            List<RedisInfo> list = Arrays.stream(urls)
                    .map(url -> {
                        String[] tmps = url.split(":");
                        RedisInfo redisInfo = new RedisInfo();
                        redisInfo.setHosts(tmps[0]);
                        redisInfo.setPorts(tmps[1]);
                        return redisInfo;
                    })
                    .collect(Collectors.toList());
            String hosts = list.stream().map(RedisInfo::getHosts).collect(Collectors.joining(","));
            String ports = list.stream().map(RedisInfo::getPorts).collect(Collectors.joining(","));
            RedisInfo clusterRedisInfo = new RedisInfo(hosts, ports, password);
            clusterRedisInfo.setCluster(true);
            redisInfos.add(clusterRedisInfo);
        }
        persistentServers(redisInfos);
        observable.notifyObservers();
    }

    public static void initServer(String hostsPorts, char[] password) {
        RedisUtils.release();
        String[] tmpArr = hostsPorts.split(":");
        if (!hostsPorts.contains(COMMA)) {
            RedisUtils.initRedis(tmpArr[0], Integer.parseInt(tmpArr[1]), password != null ? new String(password) : null);
        } else {
            RedisUtils.initRedisCluster(tmpArr[0], tmpArr[1], password != null ? new String(password) : null);
        }
    }

    private static void initRedis(String ip, int port, String password) {
        jedis = new Jedis(ip, port);
        if (StringUtil.isNotEmpty(password)) {
            jedis.auth(password.trim());
        }
        initCommandLine(ip, port, password);
        jedisCommands = jedis;
        useSingle = true;
        initialize = true;
    }

    private static void initRedisCluster(String ips, String ports, String password) {
        Set<HostAndPort> set = new HashSet<>();
        String[] tmpIps = ips.split(",");
        String[] tmpPorts = ports.split(",");
        jedisClusterList = new ArrayList<>();
        for (int i = 0; i < tmpIps.length; i++) {
            HostAndPort hostAndPort = new HostAndPort(tmpIps[i], Integer.parseInt(tmpPorts[i]));
            set.add(hostAndPort);

            jedis = new Jedis(tmpIps[i], Integer.parseInt(tmpPorts[i]));
            if (StringUtil.isNotEmpty(password)) {
                jedis.auth(password.trim());
            }
            jedisClusterList.add(jedis);
        }
        GenericObjectPoolConfig<String> config = new GenericObjectPoolConfig<>();
        jedisCluster = new JedisCluster(set, 20000, 20000, 5, password, config);
        jedisCommands = jedisCluster;
        useSingle = false;
        initialize = true;
    }

    private static List<RedisInfo> getDefRedisInfo() {
        List<RedisInfo> redisInfos = getServers();
        if (redisInfos.isEmpty()) {
            String ip = "127.0.0.1";
            String port = "6379";
            String pwd = "123456";
            redisInfos.add(new RedisInfo(ip, port, pwd));
        }
        return redisInfos;
    }

    public static Map<String, String> getRedisDbInfos() {
        int dbCount = 0;
        Map<String, String> dbInfosMap = new LinkedHashMap<>(30);
        if (useSingle) {
            while (true) {
                try {
                    if ("OK".equals(((BasicCommands) jedisCommands).select(dbCount))) {
                        dbInfosMap.put("db" + dbCount, ((BasicCommands) jedisCommands).dbSize().toString());
                        dbCount++;
                    }
                } catch (Exception e) {
                    return dbInfosMap;
                }
            }
        } else {
            dbInfosMap.put("db" + dbCount, "-1");
            return dbInfosMap;
        }
    }

    public static List<String> getRandomKeys(int dbCount) {
        if (useSingle) {
            ((BasicCommands) jedisCommands).select(dbCount);
            return getRandomKeys(jedis);
        } else {
            List<String> list = new ArrayList<>();
            for (Jedis jedis : jedisClusterList) {
                jedis.select(0);
                list.addAll(getRandomKeys(jedis));
            }
            return list;
        }
    }

    @SuppressWarnings("ALL")
    public static List<String> getRandomKeys(Jedis jedis) {
        int max = 50;
        if (jedis.dbSize() > max) {
            Pipeline pipeline = jedis.pipelined();
            for (int i = 0; i < max; i++) {
                pipeline.randomKey();
            }
            return (List<String>) ((List) pipeline.syncAndReturnAll());
        } else {
            return new ArrayList<>(jedis.keys("*"));
        }
    }


    public static Set<String> getKeys(int dbCount, String key) {
        if (useSingle) {
            ((BasicCommands) jedisCommands).select(dbCount);
            return getKeys(jedis, key);
        } else {
            Set<String> set = new HashSet<>();
            for (Jedis jedis : jedisClusterList) {
                jedis.select(0);
                set.addAll(getKeys(jedis, key));
            }
            return set;
        }
    }

    public static Set<String> getKeys(Jedis jedis, String key) {
        return jedis.keys("*" + key + "*");
    }

    public static String getValue(int dbCount, String key) {
        if (useSingle) {
            ((BasicCommands) jedisCommands).select(dbCount);
        }
        String type = jedisCommands.type(key);
        String value;
        if ("hash".equals(type)) {
            Map<String, String> map = jedisCommands.hgetAll(key);
            value = "数据类型hash:\r\n" + map.toString();
        } else if ("string".equals(type)) {
            value = "数据类型string:\r\n" + jedisCommands.get(key);
        } else if ("list".equals(type)) {
            value = "数据类型list:\r\n" + jedisCommands.lrange(key, 0, -1).toString();
        } else if ("set".equals(type)) {
            value = "数据类型set:\r\n" + jedisCommands.smembers(key).toString();
        } else if ("zset".equals(type)) {
            value = "数据类型zset:\r\n" + jedisCommands.zrange(key, 0, -1).toString();
        } else {
            value = "unknown type!";
        }
        return value;
    }

    public static void deleteAllKeys(int dbCount) {
        if (useSingle) {
            ((BasicCommands) jedisCommands).select(dbCount);
            ((BasicCommands) jedisCommands).flushDB();
        }
    }

    public static void deleteKey(int dbCount, String... keys) {
        if (useSingle) {
            ((BasicCommands) jedisCommands).select(dbCount);
        }
        for (String str : keys) {
            jedisCommands.del(str);
        }

    }

    public static String exec(String command, int dbCount) {
        Process process;
        try {
            String[] cmds;
            String os = System.getProperty("os.name");
            if (os.contains(OS_WINDOWS)) {
                cmds = new String[]{"cmd", "/C", "D:\\Redis\\redis-cli.exe " + winCommandline + " -n " + dbCount + " " + command};
            } else {
                cmds = new String[]{"/bin/sh", "-c", linuxCommandline + " -n " + dbCount + " " + command};
            }
            process = Runtime.getRuntime().exec(cmds);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BufferedReader input;
        input = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        return input.lines().collect(Collectors.joining(","));
    }

    public static void set(int dbCount, String key, String valueOrHkey, String hvalue) {
        if (useSingle) {
            ((BasicCommands) jedisCommands).select(dbCount);
        }
        if (StringUtil.isNotEmpty(hvalue)) {
            jedisCommands.hset(key, valueOrHkey, hvalue);
        } else {
            jedisCommands.set(key, valueOrHkey);
        }

    }

    private static void persistentServers(List<RedisInfo> redisInfos) {
        try {
            Preferences preferences = Preferences.userRoot();
            preferences.put(REDIS_CONFIGS, objectMapper.writeValueAsString(redisInfos));
        } catch (JsonProcessingException e1) {
            throw new RuntimeException(e1);
        }
    }

    private static List<RedisInfo> getServers() {
        Preferences preferences = Preferences.userRoot();
        String redisStrs = preferences.get(REDIS_CONFIGS, "");
        List<RedisInfo> redisInfos;
        if ("".equals(redisStrs)) {
            redisInfos = new ArrayList<>();
        } else {
            try {
                redisInfos = objectMapper.readValue(redisStrs, new TypeReference<List<RedisInfo>>() {
                });
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
        Map<Boolean, List<RedisInfo>> map = redisInfos.stream().collect(Collectors.partitioningBy(RedisInfo::isDef));
        redisInfos.clear();
        redisInfos.addAll(map.get(true));
        redisInfos.addAll(map.get(false));
        return redisInfos;
    }

    public static void release() {
        initialize = false;
        if (useSingle) {
            if (jedis != null) {
                jedis.close();
            }
        } else {
            if (jedisCluster != null) {
                try {
                    jedisCluster.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
