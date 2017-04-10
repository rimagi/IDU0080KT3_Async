package ee.ttu.idu0080.raamatupood.client;

import java.math.BigDecimal;
import java.util.Date;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import ee.ttu.idu0080.raamatupood.client.Vabrik.MessageListenerImpl;
import ee.ttu.idu0080.raamatupood.server.EmbeddedBroker;
import ee.ttu.idu0080.raamatupood.types.Car;
import ee.ttu.idu0080.raamatupood.types.Tellimus;
import ee.ttu.idu0080.raamatupood.types.TellimuseRida;
import ee.ttu.idu0080.raamatupood.types.Toode;
import ee.ttu.idu0080.raamatupood.types.Vastus;

/**
 * 
 * @author Allar Tammik
 * @date 08.03.2010
 */
public class Pood {
	private static final Logger log = Logger.getLogger(Pood.class);
	public static final String SUBJECT = "tellimuse.edastamine"; // päringu queue nimi
	public static final String RESPONSE = "tellimuse.vastus"; // vastuse queue nimi

	private String user = ActiveMQConnection.DEFAULT_USER;// brokeri jaoks vaja
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;

	long sleepTime = 1000; // 1000ms

	private int messageCount = 10;
	private long timeToLive = 1000000;
	private String url = EmbeddedBroker.URL;

	public static void main(String[] args) {
		Pood producerTool = new Pood();
		producerTool.run();
	}

	public void run() {
		Connection connection = null;
		try {
			log.info("Connecting to URL: " + url);
			log.debug("Sleeping between publish " + sleepTime + " ms");
			if (timeToLive != 0) {
				log.debug("Messages time to live " + timeToLive + " ms");
			}

			// 1. Loome ühenduse
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, password, url);
			connection = connectionFactory.createConnection();
			// Käivitame yhenduse
			connection.start();

			// 2. Loome sessiooni
			/*
			 * createSession võtab 2 argumenti: 1. kas saame kasutada
			 * transaktsioone 2. automaatne kinnitamine
			 */
			Session session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);

			// Loome teadete sihtkoha (järjekorra). Parameetriks järjekorra nimi
			Destination request = session.createQueue(SUBJECT);
			Destination response = session.createQueue(RESPONSE);

			// 3. Loome teadete saatja ja vastuv�tja
			MessageProducer producer = session.createProducer(request);
			MessageConsumer consumer = session.createConsumer(response);

			//producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(timeToLive);
			
			// Kui teade vastu võetakse käivitatakse onMessage()
			consumer.setMessageListener(new MessageListenerImpl());

			// 4. teate saatmine 
			sendTellimus(session, producer);
			getAnswer(consumer);


		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	
	protected void sendTellimus(Session session, MessageProducer producer) throws Exception {
		// loo ja saada esimene tellimus
		ObjectMessage objectMessage = session.createObjectMessage();
		Tellimus tl = new Tellimus();
		Toode toode = new Toode(1,"toode5", new BigDecimal(0.0));
		TellimuseRida tr = new TellimuseRida(toode, new Long(2));
		tl.addTellimuseRida(tr);
		toode = new Toode(2,"toode2", new BigDecimal(0.0));
		tr = new TellimuseRida(toode, new Long(3));
		tl.addTellimuseRida(tr);
		objectMessage.setObject(tl); 
		log.info("Sending msg1");
		producer.send(objectMessage);
		Thread.sleep(1000);
		// saada teine tellimus - ühegi väljata 
		Tellimus tl2 = new Tellimus();
		objectMessage.setObject(tl2); 
		log.info("Sending msg2");
		producer.send(objectMessage);
		Thread.sleep(1000);

		// saada kolmas tellimus - vigase toote nimega  
		Tellimus tl3 = new Tellimus();
		tl3.addTellimuseRida(new TellimuseRida(new Toode(1,"toode", new BigDecimal(0.0)), new Long(2)));
		objectMessage.setObject(tl3); 
		log.info("Sending msg3");
		producer.send(objectMessage);
		Thread.sleep(1000);

		// saada neljas tellimus - kolme tootega   
		Tellimus tl4 = new Tellimus();
		tl4.addTellimuseRida(new TellimuseRida(new Toode(1,"toode11", new BigDecimal(0.0)), new Long(1)));
		tl4.addTellimuseRida(new TellimuseRida(new Toode(1,"toode8", new BigDecimal(0.0)), new Long(5)));
		tl4.addTellimuseRida(new TellimuseRida(new Toode(1,"toode18", new BigDecimal(0.0)), new Long(3)));
		objectMessage.setObject(tl4); 
		log.info("Sending msg4");
		producer.send(objectMessage);

	}
	
	
	protected void getAnswer(MessageConsumer consumer)
		throws Exception {
		// Kui teade vastu võetakse käivitatakse onMessage()
		consumer.setMessageListener(new MessageListenerImpl());
	}
	
	protected void sendLoop(Session session, MessageProducer producer)
			throws Exception {

		for (int i = 0; i < messageCount || messageCount == 0; i++) {
			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(new Car(5)); // peab olema Serializable
			producer.send(objectMessage);

			TextMessage message = session
					.createTextMessage(createMessageText(i));
			log.debug("Sending message: " + message.getText());
			producer.send(message);
			
			// ootab 1 sekundi
			Thread.sleep(sleepTime);
		}
	}

	/**
	 */
	class MessageListenerImpl implements javax.jms.MessageListener {

		public void onMessage(Message message) {
			try {
				if (message instanceof TextMessage) {
					TextMessage txtMsg = (TextMessage) message;
					String msg = txtMsg.getText();
					log.info("Received: " + msg);
				} else if (message instanceof ObjectMessage) {
					ObjectMessage objectMessage = (ObjectMessage) message;
					Vastus resp = (Vastus)objectMessage.getObject();
					String msg = objectMessage.getObject().toString();
					log.info("Received: " + msg);
					if (resp.tulemus)
						log.info(String.format("Hind: %f", resp.koondHind.doubleValue()));
					else
						log.info("Vabrikust viga: " + resp.veaKirjeldus);

				} else {
					log.info("Received: " + message);
				}

			} catch (JMSException e) {
				log.warn("Caught: " + e);
				e.printStackTrace();
			}
		}
	}

	
	private String createMessageText(int index) {
		return "Message: " + index + " sent at: " + (new Date()).toString();
	}
}


