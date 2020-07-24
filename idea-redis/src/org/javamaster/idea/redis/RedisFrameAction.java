package org.javamaster.idea.redis;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;


/**
 * @author yudong
 * @date 2020/7/23
 */
public class RedisFrameAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        RedisFrame.createFrame(event.getPresentation().getText());
    }

}



