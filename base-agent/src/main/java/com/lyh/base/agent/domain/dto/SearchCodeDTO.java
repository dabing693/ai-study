package com.lyh.base.agent.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author lengYinHui
 * @date 2026/1/25
 */
@Data
public  class SearchCodeDTO {
    private String code;
    private String msg;
    private Data data;

    @lombok.Data
    public static class Data {
        private Result result;
        private List<ResponseCondition> responseConditionList;
        private String parserTreeMd5;
        private String searchTreeMd5;
        private String xcId;
        private String traceId;
        private String quoteTraceId;
        private TraceInfo traceInfo;
        private boolean hiddenDrawLine;
        private CorrectRes correctRes;
        private String ctraceId;

        @lombok.Data
        public static class Result {
            private List<Column> columns;
            private List<Map<String, Object>> dataList;
            private int total;

            @lombok.Data
            public static class Column {
                private String title;
                private String key;
                private String dateMsg;
                private boolean sortable;
                private boolean light;
                private String sortWay;
                private String indexName;
                private boolean redGreenAble;
                private String unit;
                private int userNeed;
                private boolean mtmKey;
                private String dataType;
                private int indexType;
            }
        }

        @lombok.Data
        public static class ResponseCondition {
            private String describe;
            private int stockCount;
            private List<Integer> childrenIdList;
            private int resultIndex;
            private int conditionId;
            private boolean isValid;
            private boolean removable;
        }

        @lombok.Data
        public static class TraceInfo {
            private int conditionId;
            private String showText;
            private String traceText;
            private List<TraceInfo> childrenInfo;
            private String etext;
        }

        @lombok.Data
        public static class CorrectRes {
            private boolean existError;
            private String correctedText;
            private int code;
            private boolean needCorrect;
            private int success;
            private int fail;
        }
    }
}
