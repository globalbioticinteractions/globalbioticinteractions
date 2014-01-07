package org.eol.globi.server;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.MapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class Config {


    @Bean
    public static PropertyPlaceholderConfigurer properties() {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setIgnoreResourceNotFound(true);
        ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        return ppc;
    }

    @Bean(destroyMethod = "shutdown")
    public GraphDatabaseService graphDb(@Value("${storeDir:target/graph.db.test}") final String storeDir) {
        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(baseDir + storeDir);
        graphDatabaseBuilder.setConfig(MapUtil.stringMap("read_only", "false"));
        return graphDatabaseBuilder.newGraphDatabase();
    }

    @Bean
    @Autowired
    public EhCacheCacheManager cacheManager(EhCacheManagerFactoryBean cacheFactory) {
        EhCacheCacheManager ehCacheCacheManager = new EhCacheCacheManager();
        ehCacheCacheManager.setCacheManager(cacheFactory.getObject());
        return ehCacheCacheManager;
    }

    @Bean
    @Autowired
    public EhCacheManagerFactoryBean cacheFactory(@Value("classpath:/ehcache.xml") Resource ehCacheConfig) {
        EhCacheManagerFactoryBean ehCacheManagerFactoryBean = new EhCacheManagerFactoryBean();
        ehCacheManagerFactoryBean.setConfigLocation(ehCacheConfig);
        return ehCacheManagerFactoryBean;
    }


}
