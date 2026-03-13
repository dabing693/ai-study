package com.lyh.base.agent.handler;

import com.lyh.base.agent.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/12 22:13
 */
@Component
public class QueryRewriteModelHandler extends ModelHandler {
    public QueryRewriteModelHandler(@Qualifier("queryRewriteModel") ChatModel queryRewriteModel) {
        super(queryRewriteModel);
    }
}
