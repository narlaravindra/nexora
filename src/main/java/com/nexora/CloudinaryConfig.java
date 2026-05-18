package com.nexora;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dc1dbq9pe");
        config.put("api_key", "227429946374953");
        config.put("api_secret", "mpMX_kVPLfJk8TN_FBfejWLZEpw");
        return new Cloudinary(config);
    }
}
