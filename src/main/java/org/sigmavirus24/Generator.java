package org.sigmavirus24;

import java.lang.Boolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.ServerMessageImpl;
import org.cometd.bayeux.Promise;
import org.cometd.common.HashMapMessage;

// class Generator
// private ScheduledExecutorService
// public void publishEvents(numberofEvents, ServerSession, ServerChannel)
// - serverChannel.publish(generateMessageToPublish) inside final Runnable generator
// - schedulerWithFixedDelay(generator, delay in milliseconds, delay in
// milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS)
// public void stop(ServerChannel)
// private Map<String, Object> generateMessageToPublish
//
public class Generator {
  private static final Logger logger = LoggerFactory.getLogger(Generator.class);
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
  private final ThreadLocalRandom rand = ThreadLocalRandom.current();
  private HashMap<ServerChannel, ScheduledFuture<?>> futures = new HashMap<ServerChannel, ScheduledFuture<?>>();

  public Generator( ) {
  }

  public void deliverEventsTo(final ServerChannel deliverTo, final ServerSession session, final int eventsPerPeriod, long initialDelayInMilliseconds, final long periodInMilliseconds) {
    logger.info("Setting up scheduler for " + deliverTo.toString());
    final Runnable generator = new Runnable() {
      public void run() {
        logger.info(String.format("Delivering %d events for %s", eventsPerPeriod, deliverTo.toString()));
        for (int i = 0; i < eventsPerPeriod; i++) {
          ServerMessageImpl event = new ServerMessageImpl() {{
            put(Message.ADVICE_FIELD, new HashMap<String, Object>() {{
              put("delay", periodInMilliseconds/2);
            }});
            setChannel(deliverTo.toString());
            setSuccessful(true);
            setClientId(session.getId());
            setData(new HashMap<String, String>() {{
              // put("data", new HashMap<String, String>() {{
              //   put("name", RandomStringUtils.random(12));
              //   put("id", String.format("%d", rand.nextInt()));
              // }});
              put("data", String.format("{\"name\":\"%s\",\"id\":%d}", RandomStringUtils.random(12), rand.nextInt()));
            }});
          }};
          deliverTo.publish(session, event, (Promise<Boolean>)Promise.NOOP);
        }
      }
    };
    futures.put(deliverTo, scheduler.scheduleAtFixedRate(generator, initialDelayInMilliseconds, periodInMilliseconds, TimeUnit.MILLISECONDS));
  }

  public void stop(ServerChannel deliverTo) {
    logger.info("Stopping delivery to " + deliverTo.toString());
    ScheduledFuture<?> fut = futures.get(deliverTo);
    while (!fut.isCancelled() && !fut.cancel(true)) {
      try {
        TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException e) {
        logger.debug("sleep interrupted");
      }
    }
    logger.info("killed our executor for " + deliverTo.toString());
  }

}
