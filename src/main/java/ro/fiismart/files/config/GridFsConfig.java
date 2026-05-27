package ro.fiismart.files.config;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;

@Configuration
public class GridFsConfig {

    @Bean
    @Primary
    public GridFSBucket gridFsBucket(MongoDatabaseFactory dbFactory) {
        return GridFSBuckets.create(dbFactory.getMongoDatabase());
    }

    @Bean
    @Qualifier("legacyUploadsGridFsBucket")
    public GridFSBucket legacyUploadsGridFsBucket(MongoDatabaseFactory dbFactory) {
        return GridFSBuckets.create(dbFactory.getMongoDatabase(), "uploads");
    }
}
