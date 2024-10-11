package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * @author zhongjie
 * @since 2024-09-20
 */
@Name("ratelimit")
@Summary("Rate limit of specified method invocation.")
@Description(Constants.EXPRESS_DESCRIPTION + Constants.EXAMPLE +
        "  ratelimit org.apache.commons.lang.StringUtils isBlank\n" +
        "  ratelimit *StringUtils isBlank params[0].length==1\n" +
        "  ratelimit org.apache.commons.lang.StringUtils isBlank -p20 -t200\n" +
        "  ratelimit -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank\n" +
        "  ratelimit javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter\n" +
        "  ratelimit OuterClass$InnerClass")
public class RateLimitCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String conditionExpress;
    private double permitsPerSecond;
    private long timeoutMs;
    private String timeoutMsg;
    private int cycle = 60;
    private boolean isRegEx = false;

    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to watch")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to watch")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "c", longName = "cycle")
    @Description("The monitor interval (in seconds), 60 seconds by default")
    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "p", longName = "permits-per-second")
    @Description("the rate of the returned RateLimiter, measured in how many permits become available per second, default 500")
    @DefaultValue("500")
    public void setPermitsPerSecond(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    @Option(shortName = "t", longName = "timeout-ms")
    @Description("The maximum time to wait for the permit. Negative values are treated as zero")
    @DefaultValue("-1")
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Option(shortName = "M", longName = "timeout-message")
    @Description("The runtime exception message when try acquire timeout,default 'rate limit try acquire fail'")
    @DefaultValue("rate limit try acquire fail")
    public void setTimeoutMsg(String timeoutMsg) {
        this.timeoutMsg = timeoutMsg;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public double getPermitsPerSecond() {
        return permitsPerSecond;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public String getTimeoutMsg() {
        return timeoutMsg;
    }

    public int getCycle() {
        return cycle;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    @Override
    protected Matcher<?> getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher<?> getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher<?> getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        final AdviceListener listener = new RateLimitAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
        /*
         * 通过handle回调，在suspend时停止timer，resume时重启timer
         */
        process.suspendHandler(event -> listener.destroy());
        process.resumeHandler(event -> listener.create());
        return listener;
    }
}
