package ee.ttu.idu0080.raamatupood.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

import ee.ttu.idu0080.raamatupood.server.EmbeddedBroker;
import ee.ttu.idu0080.raamatupood.types.*;

/**
 * 
 * @author Allar Tammik
 * @date 08.03.2010
 */
public class Vabrik {
	private static final Logger log = Logger.getLogger(Vabrik.class);
	private String SUBJECT = "tellimuse.edastamine"; // päringu queue nimi
	private String RESPONSE = "tellimuse.vastus"; // vastuse queue nimi
	
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;
	private String url = EmbeddedBroker.URL;

	private List<Toode> tooted;
	private MessageProducer producer = null;
	private Session session = null; 
	
	public static void main(String[] args) {
		Vabrik consumerTool = new Vabrik();
		consumerTool.run();
	}

	public void run() {
		Connection connection = null;
		try {
			log.info("Connecting to URL: " + url);
			log.info("Consuming queue : " + SUBJECT);
			
			tooted = new ArrayList<Toode>();
			for (int i = 0; i < 20; i++)
			{
				Toode temp = new Toode(i+1, String.format("toode%d", i+1), new BigDecimal(i*2+0.5));
				tooted.add(temp);
			}

			// 1. Loome ühenduse
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, password, url);
			connection = connectionFactory.createConnection();

			// Kui ühendus kaob, lõpetatakse Consumeri töö veateatega.
			connection.setExceptionListener(new ExceptionListenerImpl());

			// Käivitame ühenduse
			connection.start();

			// 2. Loome sessiooni
			// createSession võtab 2 argumenti: 1. kas saame kasutada
			// transaktsioone 2. automaatne kinnitamine
			
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// Loome teadete sihtkoha (järjekorra). Parameetriks järjekorra nimi
			Destination request = session.createQueue(SUBJECT);
			Destination response = session.createQueue(RESPONSE);

			// 3. Teadete vastuvõtja
			MessageConsumer consumer = session.createConsumer(request);
			producer = session.createProducer(response);

			// Kui teade vastu võetakse käivitatakse onMessage()
			consumer.setMessageListener(new MessageListenerImpl());

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public void sendAnswer(Tellimus tellimus) throws JMSException, InterruptedException
	{
		Thread.sleep(2000);
		List <TellimuseRida> tr = tellimus.getTellimuseRead();
		Vastus vastus = new Vastus(); 
		
		boolean toodeOlemas = false;
		if (tr.size() == 0){
			vastus.setError("Tellimuses puudusid tooted");
			log.info("Tellimuses puudusid tooted");
		}
		else{
			double sum = 0;  
			String puububToode = "";
			for (int i = 0; i < tr.size(); i++){
				toodeOlemas = false;
				log.info("Otsin toodet : " + tr.get(i).toode.nimetus);
				for (int j = 0; j < tooted.size(); j++){
					// Kas kontroll peaks olema nime või koodi põhine või mõlemat pidi ? 
//					log.info("Vordlen: " + tooted.get(j).nimetus + ". -> " + tr.get(i).toode.nimetus + ".");
					if (tooted.get(j).nimetus.equals(tr.get(i).toode.nimetus)){
						log.info(String.format("Lisan toote %s : kogus %d : hind %f", tr.get(i).toode.nimetus, tr.get(i).kogus, tooted.get(j).hind.doubleValue()));
						toodeOlemas = true;
						sum += tr.get(i).kogus * tooted.get(j).hind.doubleValue();
						break;
					}
				}
				if (!toodeOlemas){
					puububToode = tr.get(i).toode.nimetus;
					log.info("Tellimuses vigane toote nimetus " + puububToode );
					break; 
				}
			}
			if (toodeOlemas){
				vastus.setHind(sum);
				log.info(String.format("Tagastan hinna: %f", sum));
			}
			else {
				vastus.setError("Tellimuses vigane toote nimetus: " + puububToode);
			}
		}
			
		ObjectMessage objectMessage = session.createObjectMessage();
		objectMessage.setObject(vastus); // peab olema Serializable
		producer.send(objectMessage);
	}
	
	/**
	 * Käivitatakse, kui tuleb sõnum
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
					sendAnswer((Tellimus)objectMessage.getObject());
					String msg = objectMessage.getObject().toString();
					log.info("Received: " + msg);
					
				} else {
					log.info("Received: " + message);
				}
			} catch (JMSException e) {
				log.warn("Caught: " + e);
				e.printStackTrace();
			} catch (InterruptedException e) {
				log.warn("Caught: " + e);
				e.printStackTrace();
			}
		}
	}

	
	
	/**
	 * Käivitatakse, kui tuleb viga.
	 */
	class ExceptionListenerImpl implements javax.jms.ExceptionListener {

		public synchronized void onException(JMSException ex) {
			log.error("JMS Exception occured. Shutting down client.");
			ex.printStackTrace();
		}
	}

}