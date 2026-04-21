package com.fiismart.backend.config;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import database.dao.MongoConnectionPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GridFsConfig {

    @Bean
    public GridFSBucket gridFsBucket() {
        return GridFSBuckets.create(
                MongoConnectionPool.getInstance().getDatabase(),
                "uploads"
        );
    }
}
