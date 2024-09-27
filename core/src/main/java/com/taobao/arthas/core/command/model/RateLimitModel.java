package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.command.monitor200.RateLimitData;

import java.util.List;

/**
 * @author zhongjie
 * @since 2024-09-27
 */
public class RateLimitModel extends ResultModel {

    private List<RateLimitData> rateLimitDataList;

    public RateLimitModel() {
    }

    public RateLimitModel(List<RateLimitData> rateLimitDataList) {
        this.rateLimitDataList = rateLimitDataList;
    }

    @Override
    public String getType() {
        return "ratelimit";
    }

    public List<RateLimitData> getRateLimitDataList() {
        return rateLimitDataList;
    }

    public void setRateLimitDataList(List<RateLimitData> rateLimitDataList) {
        this.rateLimitDataList = rateLimitDataList;
    }
}

