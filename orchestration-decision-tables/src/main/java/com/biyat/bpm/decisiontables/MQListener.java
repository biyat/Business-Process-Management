package com.biyat.bpm.decisiontables;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.engine.impl.util.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Biyatpragyan Mohanty
 *
 */
@Component
public class MQListener {

	@Autowired
	DmnEngine dmnEngine;

	private CountDownLatch latch = new CountDownLatch(1);

	private static final Logger LOG = Logger.getLogger(MQListener.class.getName());

	public void receiveMessage(byte[] message) {
		LOG.info("Received <" + new String(message) + ">");

		try {
			JSONObject jsonMessage = new JSONObject(new String(message));

			LOG.info("Decision Tables");
			LOG.info("===============");

			DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

			LOG.info("Executing system invocation rules...");
			/**
			 * Trying to decide which decision table is needed.
			 */
			List<Map<String, Object>> result1 = dmnRuleService.createExecuteDecisionBuilder()
					.decisionKey("System_Invocation_Decision_Table")
					.variable("sourceSystem", jsonMessage.get("sourceSystem")).execute();
			String nextDecisionTable = result1.get(0).get("destinationDecisionTableReference").toString();
			LOG.info("Intermediate Decision Table to be invoked=" + nextDecisionTable);
			LOG.info("Executing intermediate rules...");
			/**
			 * Executing Decision Table 2
			 */
			List<Map<String, Object>> result2 = dmnRuleService.createExecuteDecisionBuilder()
					.decisionKey(nextDecisionTable).variable("goldMember", jsonMessage.get("goldMember"))
					.variable("shoppingCartAmount", jsonMessage.get("shoppingCartAmount")).execute();
			LOG.info("Csutomer is eligible to get " + result2.get(0).get("decision"));

			latch.countDown();
		} catch (Exception e) {
			e.printStackTrace();
			LOG.severe("Wrong Message Format");
		}

	}

	public CountDownLatch getLatch() {
		return latch;
	}

}
