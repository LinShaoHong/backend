package com.github.sun.word.loader;

import lombok.Data;

@Data
public class WordLoaderConfig {
    private Ai ai;
    private Ali ali;

    @Data
    public static class Ai {
        private String use;
        private Param qwen;
        private Param doubao;

        @Data
        public static class Param {
            private String key;
            private String model;
        }
    }

    @Data
    public static class Ali {
        private Cloud cloud;

        @Data
        public static class Cloud {
            private AccessKey accessKey;

            @Data
            public static class AccessKey {
                private String id;
                private String secret;
            }
        }
    }
}