package com.github.cheukbinli.original.common.weixin.mp;

import com.github.cheukbinli.original.common.weixin.mp.model.MessageEventModel;

public interface MessageEventHandleManager {

	MessageEventModel pushMessage(byte[] data) throws Throwable;

    void pushMessage(MessageEventModel message) throws Throwable;

    int getProcessors();

    void start(int processors);

    void start();

    void shutdown();

}
