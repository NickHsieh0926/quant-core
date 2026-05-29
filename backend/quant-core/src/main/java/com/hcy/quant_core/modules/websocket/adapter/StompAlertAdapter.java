package com.hcy.quant_core.modules.websocket.adapter;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.websocket.model.AlertTopicType;
import com.hcy.quant_core.modules.websocket.model.AlertType;
import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;
import com.hcy.quant_core.modules.websocket.port.IAlertPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class StompAlertAdapter implements IAlertPublisher {
	private static final Logger LOGGER = LoggerFactory.getLogger(StompAlertAdapter.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final SimpMessagingTemplate messagingTemplate;

	public StompAlertAdapter(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void publish(SignalAlertPayload payload) {
		switch (payload.type()) {
			case "STAT_ARB" -> {
				LOGGER.info("[Stomp publish STAT_ARB] path:{}", AlertTopicType.STAT_ARB.path());
				messagingTemplate.convertAndSend(AlertTopicType.STAT_ARB.path(), payload);
				if (payload.alertType() != AlertType.NONE) {
					messagingTemplate.convertAndSend(AlertTopicType.ALERT.path(), payload);
				}
			}
			case "ON_CHAIN" -> {
				LOGGER.info("[Stomp publish ON_CHAIN] path:{}", AlertTopicType.ON_CHAIN.path());
				messagingTemplate.convertAndSend(AlertTopicType.ON_CHAIN.path(), payload);
				if (payload.alertType() != AlertType.NONE) {
					messagingTemplate.convertAndSend(AlertTopicType.ALERT.path(), payload);
				}
			}
		}
	}
}
