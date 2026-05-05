package ro.fiismart.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConfig {

    /**
     * Suprascriem MappingMongoConverter pentru a elimina câmpul `_class`
     * din toate documentele MongoDB. Câmpul este necesar doar pentru
     * polimorfism (clase derivate în aceeași colecție), care nu este cazul nostru.
     */
    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory mongoFactory,
            MongoMappingContext context,
            MongoCustomConversions conversions) {

        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);
        converter.setCustomConversions(conversions);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }
}
