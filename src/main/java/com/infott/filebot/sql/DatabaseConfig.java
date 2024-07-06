package com.infott.filebot.sql;

public enum DatabaseConfig {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/yourDatabase", "username", "password"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://localhost:5432/yourDatabase", "username", "password"),
    ORACLE("Oracle", "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@localhost:1521:yourDatabase", "username", "password");

    private final String name;
    private final String driverClassName;
    private final String url;
    private final String username;
    private final String password;

    DatabaseConfig(String name, String driverClassName, String url, String username, String password) {
        this.name = name;
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return name;
    }
}
