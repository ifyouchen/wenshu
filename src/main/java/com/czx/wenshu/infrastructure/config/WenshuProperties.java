package com.czx.wenshu.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wenshu")
public class WenshuProperties {

    private String productName;
    private String apiVersion;
    private String baseUrl;
    private Llm llm;
    private Storage storage;
    private Mail mail;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public static class Llm {

        private String creativeModel;
        private String utilityModel;
        private String anthropicApiKey;
        private String deepseekApiKey;

        public String getCreativeModel() {
            return creativeModel;
        }

        public void setCreativeModel(String creativeModel) {
            this.creativeModel = creativeModel;
        }

        public String getUtilityModel() {
            return utilityModel;
        }

        public void setUtilityModel(String utilityModel) {
            this.utilityModel = utilityModel;
        }

        public String getAnthropicApiKey() {
            return anthropicApiKey;
        }

        public void setAnthropicApiKey(String anthropicApiKey) {
            this.anthropicApiKey = anthropicApiKey;
        }

        public String getDeepseekApiKey() {
            return deepseekApiKey;
        }

        public void setDeepseekApiKey(String deepseekApiKey) {
            this.deepseekApiKey = deepseekApiKey;
        }
    }

    public static class Storage {

        private String provider;
        private String region;
        private String bucket;
        private String secretId;
        private String secretKey;
        private String customDomain;
        private int presignedUrlTtlMinutes = 60;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getCustomDomain() {
            return customDomain;
        }

        public void setCustomDomain(String customDomain) {
            this.customDomain = customDomain;
        }

        public int getPresignedUrlTtlMinutes() {
            return presignedUrlTtlMinutes;
        }

        public void setPresignedUrlTtlMinutes(int presignedUrlTtlMinutes) {
            this.presignedUrlTtlMinutes = presignedUrlTtlMinutes;
        }
    }

    public static class Mail {

        private String from;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }
}