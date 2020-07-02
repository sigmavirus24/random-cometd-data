package org.sigmavirus24.cometd.randomdata;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;

import org.sigmavirus24.Generator;

public class GeneratorService extends AbstractService {
  private static final Logger logger = LoggerFactory.getLogger(GeneratorService.class);

  public GeneratorService(BayeuxServer bayeux) {
    super(bayeux, "events");
    logger.info("inside GeneratorService");
    //addService("/events/generator", "processGenerator");
    this.createSubscriptionListener();
  }

  private void createSubscriptionListener() {
    logger.info("Making subscription listener");
    final BayeuxServer bayeux = this.getBayeux();

    bayeux.addListener(new BayeuxServer.SubscriptionListener() {
      private final Logger subLogger = LoggerFactory.getLogger("SubscriptionListener");
      private final Generator gen = new Generator();

      @Override
      public void subscribed(ServerSession serverSession, ServerChannel serverChannel, ServerMessage serverMessage) {
        subLogger.info("Subscription created to " + serverChannel.toString());
        final String serverChannelName = serverChannel.toString();
        if (!serverChannelName.startsWith("/events/")) {
          return;
        }

        final String eventsInfo = serverChannelName.replaceFirst("/events/", "");
        int eventsPerConnect;
        try {
          eventsPerConnect = Integer.parseInt(eventsInfo);
        } catch (NumberFormatException e) {
          eventsPerConnect = 10;
        }
        gen.deliverEventsTo(serverChannel, serverSession, eventsPerConnect, 0, 9500);
        subLogger.info(String.format("Channel subscribed for %d events per /meta/connect", eventsPerConnect));
      }

      @Override
      public void unsubscribed(ServerSession serverSession, ServerChannel serverChannel, ServerMessage serverMessage) {
        subLogger.info("unsubscribed from " + serverChannel.toString());
      }
    });
  }
}

