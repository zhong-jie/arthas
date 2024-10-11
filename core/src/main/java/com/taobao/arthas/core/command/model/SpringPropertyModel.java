package com.taobao.arthas.core.command.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zhongjie
 * @since 2024-09-27
 */
public class SpringPropertyModel extends ResultModel {

    private final List<SpringPropertiesVO> springPropertiesVOList = new LinkedList<>();

    public void addSpringPropertiesVO(SpringPropertiesVO springPropertiesVO) {
        springPropertiesVOList.add(springPropertiesVO);
    }

    public List<SpringPropertiesVO> getSpringPropertiesVOList() {
        return springPropertiesVOList;
    }

    @Override
    public String getType() {
        return "springprop";
    }
}
