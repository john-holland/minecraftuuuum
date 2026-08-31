package com.minecraftuuuum.server;

import com.unimined.craftpressor.db.CraftpressorDb;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.sql.SQLException;

@Configuration
public class AppConfig {
    @Bean
    public CraftpressorDb craftpressorDb(@Value("${minecraftuuuum.db}") String db) throws SQLException {
        return new CraftpressorDb(Path.of(db));
    }
}
