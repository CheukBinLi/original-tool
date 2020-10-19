package com.github.cheukbinli.original;

import com.github.cheukbinli.original.qrcode.Qrcode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

//@EnableScheduling
//@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@SpringBootApplication
@Configuration
@ComponentScan
//@PropertySource(value = { "classpath:i18n.properties" }, encoding = "utf-8")
public class ApiApplication extends SpringBootServletInitializer implements InitializingBean {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ApiApplication.class);
    }

    @Bean
    Qrcode getQrcode() {
        return new Qrcode();
    }

    @SneakyThrows
    @Override
    public void afterPropertiesSet() throws Exception {
        System.err.println("1");
        System.err.println("data:image/png;base64," + Qrcode.getQrcode64("chrome-extension://klbibkeccnjlkjkiokjodocebajanakg/suspended.html#ttl=%E6%9C%89%E9%81%93%E4%BA%91%E7%AC%94%E8%AE%B0&pos=0&uri=https://note.youdao.com/ynoteshare1/index.html?id=dd9ec8a63ab3b97eba58bf7b32e4a8d9&type=notehare1/index.html?id=dd9ec8a63ab3b97eba58bf7b32e4a8d9&type=notehare1/index.html?id=dd9ec8a63ab3b97eba58bf7b32e4a8d9&type=note"));
        System.err.println("2");
    }
}
