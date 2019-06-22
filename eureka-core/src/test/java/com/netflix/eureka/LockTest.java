package com.netflix.eureka;

import org.junit.Test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @ClassName LockTest
 * @Description TODO
 * @Author xiaof
 * @Date 2019/6/22 20:07
 * @Version 1.0
 **/
public class LockTest {

    @Test
    public void test1() {
        //测试一下读写锁的作用
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Lock readLock = readWriteLock.readLock();
        Lock writeLock = readWriteLock.writeLock();

        //这里起个线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                //如果我们先上读锁，然后看看写锁会干嘛
                System.out.println(Thread.currentThread().getName() + "尝试上读锁:" + System.currentTimeMillis());
                readLock.lock();
                System.out.println(Thread.currentThread().getName() + "上读锁10秒:" + System.currentTimeMillis());
                try {
                    //10s
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 放开读锁：" + System.currentTimeMillis());
                readLock.unlock();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //如果我们先上读锁，然后看看写锁会干嘛
                System.out.println(Thread.currentThread().getName() + "尝试上读锁:" + System.currentTimeMillis());
                readLock.lock();
                System.out.println(Thread.currentThread().getName() + "上读锁10秒:" + System.currentTimeMillis());
                try {
                    //10s
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 放开读锁：" + System.currentTimeMillis());
                readLock.unlock();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //如果我们先上读锁，然后看看写锁会干嘛
                System.out.println(Thread.currentThread().getName() + "尝试上写锁:" + System.currentTimeMillis());
                writeLock.lock();
                System.out.println(Thread.currentThread().getName() + "上写锁10秒:" + System.currentTimeMillis());
                try {
                    //10s
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 放开写锁：" + System.currentTimeMillis());
                writeLock.unlock();
            }
        }).start();


        try {
            //10s
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
