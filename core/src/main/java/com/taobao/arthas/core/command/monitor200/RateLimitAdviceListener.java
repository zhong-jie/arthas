package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.advisor.RethrowException;
import com.taobao.arthas.core.command.model.RateLimitModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import google.guava.com.google.common.util.concurrent.RateLimiter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;

/**
 * @author zhongjie
 * @since 2024-09-20
 */
public class RateLimitAdviceListener  extends AdviceListenerAdapter {

    public RateLimitAdviceListener(RateLimitCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
        this.rateLimiter = RateLimiter.create(command.getPermitsPerSecond());
    }

    private final RateLimitCommand command;
    private final CommandProcess process;
    private final RateLimiter rateLimiter;

    // 输出定时任务
    private Timer timer;
    private final ConcurrentHashMap<Key, AtomicReference<RateLimitData>> rateLimitData = new ConcurrentHashMap<>();

    @Override
    public synchronized void create() {
        if (timer == null) {
            timer = new Timer("Timer-for-arthas-ratelimit-" + process.session().getSessionId(), true);
            timer.scheduleAtFixedRate(new RateLimitTimer(rateLimitData, process),
                    0, command.getCycle() * 1000L);
        }
    }

    @Override
    public synchronized void destroy() {
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
        Advice advice = Advice.newForBefore(loader, clazz, method, target, args);
        boolean conditionResult = isConditionMet(command.getConditionExpress(), advice, 0);
        if (this.isVerbose()) {
            process.write("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
        }
        if (conditionResult) {
            boolean timeout = false;
            if (command.getTimeoutMs() == -1) {
                rateLimiter.acquire();
                if (this.isVerbose()) {
                    process.write("acquire success, permit:" + rateLimiter.getRate() + "\n");
                }
            } else {
                timeout = !rateLimiter.tryAcquire(command.getTimeoutMs(), TimeUnit.MILLISECONDS);
                if (timeout) {
                    if (this.isVerbose()) {
                        process.write("Timeout when try acquire, permit:" + rateLimiter.getRate() + " timeout:" +
                                command.getTimeoutMs() + "ms\n");
                    }

                }
            }

            statRateLimitData(clazz, method, timeout);

            if (timeout) {
                throw new RethrowException(new RuntimeException(command.getTimeoutMsg()));
            }
        }
    }

    private void statRateLimitData(Class<?> clazz, ArthasMethod method, boolean timeout) {
        final Key key = new Key(clazz.getName(), method.getName());

        while (true) {
            AtomicReference<RateLimitData> value = rateLimitData.get(key);
            if (null == value) {
                rateLimitData.putIfAbsent(key, new AtomicReference<>(new RateLimitData()));
                continue;
            }

            while (true) {
                RateLimitData oData = value.get();
                RateLimitData nData = new RateLimitData();
                nData.setTimestamp(LocalDateTime.now());
                if (timeout) {
                    nData.setFailed(oData.getFailed() + 1);
                    nData.setSuccess(oData.getSuccess());
                } else {
                    nData.setFailed(oData.getFailed());
                    nData.setSuccess(oData.getSuccess() + 1);
                }
                nData.setTotal(oData.getTotal() + 1);
                if (value.compareAndSet(oData, nData)) {
                    break;
                }
            }
            break;
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {
        // nothing
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
        // nothing
    }

    private static class RateLimitTimer extends TimerTask {
        private final Map<Key, AtomicReference<RateLimitData>> rateLimitData;
        private final CommandProcess process;

        RateLimitTimer(Map<Key, AtomicReference<RateLimitData>> rateLimitData, CommandProcess process) {
            this.rateLimitData = rateLimitData;
            this.process = process;
        }

        @Override
        public void run() {
            if (rateLimitData.isEmpty()) {
                return;
            }

            process.times().getAndIncrement();

            List<RateLimitData> rateLimitDataList = new ArrayList<>(rateLimitData.size());
            for (Map.Entry<Key, AtomicReference<RateLimitData>> entry : rateLimitData.entrySet()) {
                final AtomicReference<RateLimitData> value = entry.getValue();

                RateLimitData data;
                do {
                    data = value.get();
                    //swap rate limit data to new instance
                } while (!value.compareAndSet(data, new RateLimitData()));

                if (null != data) {
                    data.setClassName(entry.getKey().getClassName());
                    data.setMethodName(entry.getKey().getMethodName());
                    rateLimitDataList.add(data);
                }
            }
            process.appendResult(new RateLimitModel(rateLimitDataList));
        }

    }

    /**
     * 数据监控用的Key
     *
     * @author vlinux
     */
    private static class Key {
        private final String className;
        private final String methodName;

        Key(String className, String behaviorName) {
            this.className = className;
            this.methodName = behaviorName;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public int hashCode() {
            return className.hashCode() + methodName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key okey = (Key) obj;
            return isEquals(okey.className, className) && isEquals(okey.methodName, methodName);
        }

    }
}