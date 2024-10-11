package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SpringPropertyModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author zhongjie
 * @since 2024-09-27
 */
public class SpringPropertyView extends ResultView<SpringPropertyModel> {
    @Override
    public void draw(CommandProcess process, SpringPropertyModel result) {
        result.getSpringPropertiesVOList().forEach(springPropertiesVO -> {
            process.write(springPropertiesVO.getApplicationContext().toString());
            process.write("\n");
            process.write(ViewRenderUtil.renderKeyValueTable(springPropertiesVO.getProperties(), process.width()));
        });

    }
}
