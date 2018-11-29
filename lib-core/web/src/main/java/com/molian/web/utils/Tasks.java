package com.molian.web.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import bolts.CancellationToken;
import bolts.CancellationTokenSource;
import bolts.Task;

public final class Tasks {

    //UI
    public static final Executor UI_EXECUTOR = Task.UI_THREAD_EXECUTOR;
    //数据库的线程池
    public static final Executor DB_EXECUTOR = Executors.newFixedThreadPool(3);

    //文件读写线程池
    public static final Executor IO_EXECUTOR = Executors.newFixedThreadPool(3);

    //网络线程池
    public static final Executor NET_EXECUTOR = Executors.newFixedThreadPool(3);

    //计算线程池
    public static final Executor COM_EXECUTOR = Executors.newCachedThreadPool();

    //取消TOken池
    private static Map<Object, CancellationTokenSource> sCancelTokenMap = new ConcurrentHashMap<Object, CancellationTokenSource>();


    /**
     * 判断Task是否执行成功
     *
     * @param task
     */
    public static boolean isSuccess(Task task) {
        if (task == null)
            return false;
        if (task.getError() != null) {
            task.getError().printStackTrace();
        }
        return !task.isCancelled() && task.isCompleted() && !task.isFaulted();
    }

    /**
     * 判断Task结果不为空
     *
     * @param task
     * @return
     */
    public static boolean isResultNotNull(Task task) {
        if (task.getError() != null) {
            task.getError().printStackTrace();
        }
        return isSuccess(task) && task.getResult() != null;
    }

    /**
     * 获取取消Token
     *
     * @param tag
     * @return
     */
    public static CancellationToken obtainCancelToken(Object tag) {
        if (tag == null) {
            tag = new Object();
        }
        CancellationTokenSource cancellationTokenSource = sCancelTokenMap.get(tag);

        if (cancellationTokenSource == null) {
            cancellationTokenSource = new CancellationTokenSource();
            sCancelTokenMap.put(tag, cancellationTokenSource);
        }
        return cancellationTokenSource.getToken();
    }

    /**
     * 取消Task
     *
     * @param tag
     */
    public static void cancelTask(Object tag) {
        CancellationTokenSource cancellationTokenSource = sCancelTokenMap.remove(tag);
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
    }


    /**
     * 取消所有Task
     */
    public static void cancelAllTask() {
        Iterator<Object> iterator = sCancelTokenMap.keySet().iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            sCancelTokenMap.get(next).cancel();
        }
        sCancelTokenMap.clear();
    }

    /**
     * 判断Task是否 成功
     *
     * @param task
     */
    public static void isSuccessOrThrow(Task<?> task) {
        if (task == null || task.getError() != null || !isSuccess(task)) {
            throw new RuntimeException(task.getError());
        }
        return;
    }
}
