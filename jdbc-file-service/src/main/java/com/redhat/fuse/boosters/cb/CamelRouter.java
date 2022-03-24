package com.redhat.fuse.boosters.cb;

import javax.sql.DataSource;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

// Not available in Camel 2.23..2  (Fuse 7.10)
import org.apache.camel.processor.idempotent.jdbc.JdbcOrphanLockAwareIdempotentRepository;

import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.apache.camel.spi.IdempotentRepository;

/**
 * A simple Camel REST DSL route that implement the name service.
 * 
 */
@Component
public class CamelRouter extends RouteBuilder {

    @Autowired
    DataSource dataSource;

    @Value("${com.redhat.test.processor.name}")
    private String processorName;


    // Reference:  https://camel.apache.org/components/3.15.x/sql-component.html#_using_the_jdbc_based_idempotent_repository
    @Bean(name="inProgressRepo")
    public JdbcOrphanLockAwareIdempotentRepository inProgressRepo() {
        JdbcOrphanLockAwareIdempotentRepository jRepo = new JdbcOrphanLockAwareIdempotentRepository(dataSource, processorName, new DefaultCamelContext());

        // Set to 5 minutes
        // If after 5 minutes of no update to createdat field in DB, then any processor can re-attempt processing of orphaned file
        jRepo.setLockMaxAgeMillis(30000l);

        jRepo.setLockKeepAliveIntervalMillis(3000L);
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
