package com.xcz.commons.oss.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.xcz.commons.oss.service.UploadService;
import com.xcz.commons.oss.service.impl.UploadServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnClass(OSS.class)
@ConditionalOnProperty(prefix = "aliyun.oss", name = "endpoint")
public class OssClientConfig {

    @Resource
    private OssProperties ossProperties;

    public OSS ossClient() {
        DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());

        //配置 V4 签名
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        //构建并返回
        return OSSClientBuilder.create()
                .endpoint(ossProperties.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(ossProperties.getRegion())
                .build();
    }

    @Bean
    public UploadService uploadService(OssProperties ossProperties) {
        Oss oss = ossClient();
        return new UploadServiceImpl(oss, ossProperties);
    }
}
