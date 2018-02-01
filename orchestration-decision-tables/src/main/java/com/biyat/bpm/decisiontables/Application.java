package com.biyat.bpm.decisiontables;

import javax.sql.DataSource;

import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.configurator.DmnEngineConfigurator;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * @author Biyatpragyan Mohanty
 *
 */
@RestController
@EnableAutoConfiguration
@ComponentScan("com.biyat.bpm")
public class Application {

	@RequestMapping("/")
	String home() {
		return "Hello World!";
	}
	
	@Autowired
	protected DataSource dataSource;
	
	@Autowired
	protected PlatformTransactionManager transactionManager;
	
	@Bean
	public SpringDmnEngineConfiguration dmnEngineConfigure() {
		SpringDmnEngineConfiguration sdec = new SpringDmnEngineConfiguration();
		sdec.setDataSource(dataSource);
		sdec.setTransactionManager(transactionManager);
		sdec.setDatabaseSchemaUpdate("true");
		sdec.setDeploymentMode("single-resource");
		return sdec;
	}
	
	@Bean
	public DmnEngineConfigurator dmnEngineConfigurator() {
		DmnEngineConfigurator dec = new DmnEngineConfigurator();
		dec.setDmnEngineConfiguration(dmnEngineConfigure());
		return dec;
	}
	
	@Bean
	public DmnEngine getDmnEngine(SpringDmnEngineConfiguration sdec) {
		return sdec.buildDmnEngine();
	}

	/**
	 * Spring boot main code execution.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

	public final static String SFG_MESSAGE_QUEUE = "data-receiver-queue";

	@Bean
	Queue queue() {
		return new Queue(SFG_MESSAGE_QUEUE, true);
	}

	/**
	 * External systems publish data to the end point provided. Depending on routing strategy these
	 * messages/data will be transmitted to respective queue(s).
	 * @return
	 */
	@Bean
	DirectExchange exchange() {
		return new DirectExchange("data-receiver-exchange");
	}

	/**
	 * An exchange is bound to a queue depending on routing key.
	 * @param queue
	 * @param exchange
	 * @return
	 */
	@Bean
	Binding binding(Queue queue, DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with("shopping-cart-data-processing");
	}

	/**
	 * Message listener export and process data from the message queues. 
	 * @param connectionFactory
	 * @param listenerAdapter
	 * @return
	 */
	@Bean
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(SFG_MESSAGE_QUEUE);
		container.setMessageListener(listenerAdapter);
		return container;
	}

	
	/**
	 * Message listener export and process data from the message queues. 
	 * @param receiver
	 * @return
	 */
	  @Bean 
	  MessageListenerAdapter listenerAdapter(MQListener receiver) { 
		  return new MessageListenerAdapter(receiver, "receiveMessage"); 
	  }
		
}
