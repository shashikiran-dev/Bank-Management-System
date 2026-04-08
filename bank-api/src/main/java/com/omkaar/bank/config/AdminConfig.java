package com.omkaar.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Admin credentials from application.properties.
 *
 * NOTE: No @Component — registered via
 * @EnableConfigurationProperties(AdminConfig.class) in CoreConfig only.
 */
@ConfigurationProperties(prefix = "bank.admin")
public class AdminConfig {

    private String username = "admin";
    private String password = "admin123";

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }

    public boolean authenticate(String user, String pass) {
        return username.equals(user) && password.equals(pass);
    }
}
