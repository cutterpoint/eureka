/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.discovery.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter implementation is based on token bucket algorithm. There are two parameters:
 * <ul>
 * <li>
 *     burst size - maximum number of requests allowed into the system as a burst
 * </li>
 * <li>
 *     average rate - expected number of requests per second (RateLimiters using MINUTES is also supported)
 * </li>
 * </ul>
 *
 *  这个是一个限流的类，通过令牌桶算法，限制接口流量
 *
 * @author Tomasz Bak
 */
public class RateLimiter {

    private final long rateToMsConversion;

    private final AtomicInteger consumedTokens = new AtomicInteger();
    private final AtomicLong lastRefillTime = new AtomicLong(0);

    @Deprecated
    public RateLimiter() {
        this(TimeUnit.SECONDS);
    }

    public RateLimiter(TimeUnit averageRateUnit) {
        switch (averageRateUnit) {
            case SECONDS:
                rateToMsConversion = 1000;
                break;
            case MINUTES:
                rateToMsConversion = 60 * 1000;
                break;
            default:
                throw new IllegalArgumentException("TimeUnit of " + averageRateUnit + " is not supported");
        }
    }

    //入参2个，令牌桶最大上限，平均速率
    public boolean acquire(int burstSize, long averageRate) {
        //获取消费令牌
        return acquire(burstSize, averageRate, System.currentTimeMillis());
    }

    public boolean acquire(int burstSize, long averageRate, long currentTimeMillis) {
        if (burstSize <= 0 || averageRate <= 0) { // Instead of throwing exception, we just let all the traffic go
            return true;
        }
        //重新填充令牌，桶大小，平均速率，当前时间戳
        refillToken(burstSize, averageRate, currentTimeMillis);
        //消费令牌
        return consumeToken(burstSize);
    }

    /**
     *
     * @program: com.netflix.discovery.util.RateLimiter
     * @description: 这里可以看到，我本来以为他会起一个定时器匀速填充令牌，但是从这里我们可以看到，并没有后台起一个定时器，进行定时填充
     *          而是通过发起消费请求的之前，先进行一波填充，只是根据当前请求的时间，批量填充这段时间平均需要填充多少令牌的算法
     * @auther: xiaof
     * @date: 2019/6/21 10:57
     */
    private void refillToken(int burstSize, long averageRate, long currentTimeMillis) {
        long refillTime = lastRefillTime.get();
        //间隔的时间
        long timeDelta = currentTimeMillis - refillTime;
        //用间隔时间*速率，然后除以我们的单位，是秒，还是分，来计算获取令牌的个数
        long newTokens = timeDelta * averageRate / rateToMsConversion;
        if (newTokens > 0) {
            long newRefillTime = refillTime == 0
                    ? currentTimeMillis
                    : refillTime + newTokens * rateToMsConversion / averageRate;
            //cas操作，避免多线程干扰,如果cas操作成功，那么就进去，每次只有单线程进入
            if (lastRefillTime.compareAndSet(refillTime, newRefillTime)) {
                while (true) {
                    //AtomicInteger 原子对象，获取当前消费的令牌
                    int currentLevel = consumedTokens.get();
                    //取当前消费的令牌数，和桶大小中小的那个，如果消费比桶还大，那么基本就是超负载了，需要全桶填充
                    int adjustedLevel = Math.min(currentLevel, burstSize); // In case burstSize decreased
                    //获取0，或者用已经消费的令牌数-新的需要入库的令牌数=新的令牌容量（也就是被消费的令牌）
                    //这一步说白了就是填充被消费的令牌的空余进去
                    int newLevel = (int) Math.max(0, adjustedLevel - newTokens);
                    //
                    if (consumedTokens.compareAndSet(currentLevel, newLevel)) {
                        return;
                    }
                }
            }
        }
    }

    private boolean consumeToken(int burstSize) {
        while (true) {
            int currentLevel = consumedTokens.get();
            if (currentLevel >= burstSize) {
                return false;
            }
            if (consumedTokens.compareAndSet(currentLevel, currentLevel + 1)) {
                return true;
            }
        }
    }

    public void reset() {
        consumedTokens.set(0);
        lastRefillTime.set(0);
    }
}
