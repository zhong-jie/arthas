package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.RateLimitModel;
import com.taobao.arthas.core.command.monitor200.RateLimitData;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.text.DecimalFormat;

import static com.taobao.text.ui.Element.label;

/**
 * @author zhongjie
 * @since 2024-09-27
 */
public class RateLimitView extends ResultView<RateLimitModel> {
    @Override
    public void draw(CommandProcess process, RateLimitModel result) {
        TableElement table = new TableElement(2, 3, 3, 1, 1, 1, 1).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("timestamp").style(Decoration.bold.bold()),
                label("class").style(Decoration.bold.bold()),
                label("method").style(Decoration.bold.bold()),
                label("total").style(Decoration.bold.bold()),
                label("success").style(Decoration.bold.bold()),
                label("fail(timeout)").style(Decoration.bold.bold()),
                label("fail-rate").style(Decoration.bold.bold()));

        final DecimalFormat df = new DecimalFormat("0.00");

        for (RateLimitData data : result.getRateLimitDataList()) {
            table.row(DateUtils.formatDateTime(data.getTimestamp()),
                    data.getClassName(),
                    data.getMethodName(),
                    "" + data.getTotal(),
                    "" + data.getSuccess(),
                    "" + data.getFailed(),
                    df.format(100.0d * div(data.getFailed(), data.getTotal())) + "%"
            );
        }

        process.write(RenderUtil.render(table, process.width()) + "\n");

    }

    private double div(double a, double b) {
        if (b == 0) {
            return 0;
        }
        return a / b;
    }
}
