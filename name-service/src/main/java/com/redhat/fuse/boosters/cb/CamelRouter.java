package com.redhat.fuse.boosters.cb;

import javax.sql.DataSource;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

// Not available in Camel 2.23..2  (Fuse 7.10)
// import org.apache.camel.processor.idempotent.jdbc.JdbcOrphanLockAwareIdempotentRepository;

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

    @Bean(name="inProgressRepo")
    public IdempotentRepository inProgressRepo() {
        IdempotentRepository jRepo = new JdbcMessageIdRepository(dataSource, "myProcessor");
        return jRepo;
    }


    @Override
    public void configure() throws Exception {

        // @formatter:off
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json);
        
        rest("/name").description("Name REST service")
            .consumes("application/json")
            .produces("application/json")

            .get().description("Generate a Name").outType(Name.class)
                .responseMessage().code(200).endResponseMessage()
                .to("bean:nameService?method=getName");
        // @formatter:on
        
        //from("file:///tmp/na/input?delay=1000&autoCreate=true&delete=false")
        from("file:///tmp/na/input?inProgressRepository=#inProgressRepo&delay=1000&autoCreate=true&delete=false")
            .routeId("direct:fileConsumer")
            .log("file = ${header.CamelFileName}}")
            .end();
    }

}
