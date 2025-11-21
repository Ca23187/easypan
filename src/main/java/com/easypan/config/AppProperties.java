package com.easypan.config;

import com.easypan.common.util.StringTools;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// NOTE: 这种配置方式好在灵活便捷，但更加规范的配置方式请参见尚庭公寓项目
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private static final Logger logger = LoggerFactory.getLogger(AppProperties.class);

    private String projectFolder;
    private boolean dev;
    private List<String> adminEmails = new ArrayList<>();
    private final Mail mail = new Mail();
    private final Qq qq = new Qq();

    @Getter
    @Setter
    public static class Mail {
        /** 发件人 */
        private String from;
    }

    @Getter
    @Setter
    public static class Qq {
        private String appId;
        private String appKey;
        private String urlAuthorization;
        private String urlAccessToken;
        private String urlOpenId;
        private String urlUserInfo;
        private String urlRedirect;
    }

    @PostConstruct
    public void normalize() {
        if (!StringTools.isEmpty(projectFolder) && !projectFolder.endsWith("/")) {
            projectFolder = projectFolder + "/";
        }
    }
}
