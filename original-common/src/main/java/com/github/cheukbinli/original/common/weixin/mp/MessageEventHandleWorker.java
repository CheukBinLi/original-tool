package com.github.cheukbinli.original.common.weixin.mp;

import com.github.cheukbinli.original.common.weixin.content.MessageType;
import com.github.cheukbinli.original.common.weixin.mp.model.MessageEventModel;

import java.util.List;
import java.util.concurrent.Callable;

public interface MessageEventHandleWorker<T> extends Callable<T>, MessageType {

    /***
     * 添加任务
     * 
     * @param messageEventModels
     */
    public void pushTask(List<MessageEventModel> messageEventModels);

    public void pushTask(MessageEventModel messageEventModels);

    /***
     * 待处理任务数量
     * 
     * @return
     */
    public int size();

    /***
     * 中断
     */
    public void interrupt();

    /***
     * 运行状态
     * 
     * @return
     */
    public boolean isActivate();

    /***
     * 处理
     * 
     * @param messageEventModel
     */
    public void process(MessageEventModel messageEventModel);

}
