package com.lyh.trade.self_react.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class FunctionTool {
    @JsonProperty(value = "type")
    private Type type = Type.FUNCTION;
    private Function function;

    public FunctionTool(Function function) {
        this.type = Type.FUNCTION;
        this.function = function;
    }

    @Data
    public static class Function {
        private String name;
        private String description;
        private Parameters parameters;

        @Data
        public static class Parameters {
            private final String type = "object";
            private Map<String, Property> properties = new HashMap<>();
            private List<String> required;

            public Parameters(Map<String, Property> properties, List<String> required) {
                this.properties = properties;
                this.required = required;
            }

            @Data
            public static class Property {
                private String type;
                private String description;
            }
        }
    }
}