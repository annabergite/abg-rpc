# 发布说明
----------------------------------------------
## abg-0.0.9
发布时间: 2018-07-26

更新说明: 
1. Client 合并发送请求数据，参考 ServiceComb 实现
2. 实现了 ManualSerializer，作为序列化性能的基准参考
3. 升级 javassist 到 3.23.1-GA
4. 升级 netty 到 4.1.27.Final

----------------------------------------------

## abg-0.0.8
发布时间: 2018-06-25

更新说明: 
1. 修复 javassist 类加载出错问题

----------------------------------------------

## abg-0.0.7
发布时间: 2018-06-25

更新说明: 
1. 重新实现了 kryo protostuff 的序列化实现，显著提高性能
2. 通过代码静态扫描，找出 rpc 方法用到的类，并注册到 kryo protostuff 中
3. 优化了 HexUtils URLEncodeUtils 的性能
4. 修复了几个移位操作 bug
5. 拆分出 abg-utils abg-kryo abg-protostuff 三个项目
6. 修改了 mvn 结构，方便 idea 导入项目
7. 升级 javassist 到 3.23.0-GA
8. 升级 netty 到 4.1.25.Final
9. 升级 spring-boot 到 2.0.3.RELEASE

----------------------------------------------

## abg-0.0.6
发布时间: 2018-05-11

更新说明: 
1. 修改 netty 配置（参考 jupiter-rpc）
2. 提高 ConcurrentArrayList 的安全性和通用性
3. 减少 Client 端 Handler 数量
4. 升级 netty 到 4.1.24
5. 升级 jackson 到 2.9.5
6. 升级 guava 到 25.0
7. 升级 spring-boot 到 2.0.2

----------------------------------------------

## abg-0.0.5
发布时间: 2018-03-31

更新说明: 
1. FastClock AtomicMuiltInteger 引入缓存行填充
2. 最大在途请求数可以配置为不做任何限制，以减少锁竞争
3. 消除 FutureContainer 的锁竞争，使用 IntObjectHashMap 替换掉 NonBlockingHashMapLong
4. 删除 jctools 依赖
5. 升级 kryo 到 4.0.2

----------------------------------------------

## abg-0.0.4
发布时间: 2018-03-21

更新说明: 
1. 升级 netty 到 4.1.22.Final
2. 升级 guava 到 24.1
3. 升级 typesafe-config 到 1.3.3
4. 升级 jctools 到 2.1.2
5. 提高 ConcurrentArrayList ConcurrentIntToIntArrayMap ConcurrentIntToObjectArrayMap 线程安全性
6. ConcurrentIntToIntArrayMap ConcurrentIntToObjectArrayMap 添加 remove 方法
7. 引入 netty Recycler，提高性能，降低垃圾回收压力
8. 兼容 Java 10
9. 兼容 SpringBoot 2

----------------------------------------------

## abg-0.0.3
发布时间: 2018-02-06

更新说明: 
1. 清理掉无用的 BlazeObjectPool 实现
2. 删除 RandomId 中的无用方法
3. 删除 ByteBufUtils 中的无用方法
4. 升级 jackson 到 2.9.4
5. 升级 guava 到 24.0
6. 优化 weight 相同情况下的 LoadBalance 性能
7. 修复重复创建 MethodParamClass 的 bug
8. 修复 App 被关掉后继续自动重连的 bug

----------------------------------------------

***END***