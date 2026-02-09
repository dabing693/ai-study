package com.lyh.base.agent.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.base.agent.enums.ToolType;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class FunctionTool {
    @JsonProperty(value = "type")
    private ToolType type;
    private Function function;

    public FunctionTool(Function function) {
        this.type = ToolType.FUNCTION;
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
            private LinkedHashMap<String, Property> properties;
            private List<String> required;

            public Parameters(LinkedHashMap<String, Property> properties, List<String> required) {
                this.properties = properties;
                this.required = required;
            }

            @Data
            public static class Property {
                private String type;
                private String description;
                @JsonIgnore
                private Class<?> clz;
            }
        }
    }
}