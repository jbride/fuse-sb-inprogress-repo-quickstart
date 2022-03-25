package com.redhat.fuse.boosters.cb;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import org.apache.camel.component.infinispan.processor.idempotent.InfinispanIdempotentRepository;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

@Component
public class CamelRouter extends RouteBuilder {

    @Value("${com.redhat.test.processor.name}")
    private String processorName;


    @Bean(name="inProgressRepo")
    public InfinispanIdempotentRepository inProgressRepo() {

        // TO-DO:  set cache entry expiry to 90 seconds
        // Set to 90 seconds
        // If after 90 seconds of no update to createdat field in DB, then any processor in cluster can re-attempt processing of orphaned file

        GlobalConfiguration global = new GlobalConfigurationBuilder()
            .globalJmxStatistics()
            .allowDuplicateDomains(true)
            .build();
        Configuration conf = new ConfigurationBuilder().build();
        BasicCacheContainer cacheContainer = new DefaultCacheManager(global, conf);
        cacheContainer.start();

        InfinispanIdempotentRepository jRepo = new InfinispanIdempotentRepository(cacheContainer, processorName);

        return jRepo;
    }


    @Override
    public void configure() throws Exception {

        // By default, Camel will move consumed files to the .camel sub-folder relative to the directory where the file was consumed.
        // Any move or delete operations is executed after (post command) the routing has completed; so during processing of the Exchange the file is still located in the inbox folder.

        //from("file:///tmp/na/input?delay=5000&autoCreate=true&delete=false")
        from("file:{{com.redhat.test.dir.location}}?inProgressRepository=#inProgressRepo&delay=5000&autoCreate=true&delete=false")
            .routeId("direct:fileConsumer")
            .log("file = ${header.CamelFileName}}")
            .delay(simple("{{com.redhat.test.delay.millis}}"))
            .end();
    }

}
