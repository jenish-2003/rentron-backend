package com.jcbbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${core.fileTransfer.primaryUploadFolder:/opt/microservice/upload/images}")
    private String primaryUploadFolder;

    private String resolveBaseDirectory() {
        String baseDirStr = primaryUploadFolder;
        try {
            java.io.File baseDir = new java.io.File(baseDirStr);
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            if (!baseDir.canWrite()) {
                baseDirStr = System.getProperty("user.dir") + java.io.File.separator + "uploads" + java.io.File.separator + "images";
                new java.io.File(baseDirStr).mkdirs();
            }
        } catch (Exception ex) {
            baseDirStr = System.getProperty("user.dir") + java.io.File.separator + "uploads" + java.io.File.separator + "images";
            new java.io.File(baseDirStr).mkdirs();
        }
        return baseDirStr;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDirStr = resolveBaseDirectory();
        java.io.File baseFolder = new java.io.File(baseDirStr);
        String location = baseFolder.toURI().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
