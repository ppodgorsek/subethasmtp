package org.subethamail.smtp.server;

import java.io.IOException;
import java.io.InputStream;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.client.SMTPException;
import org.subethamail.smtp.client.SmartClient;
import org.subethamail.smtp.util.TextUtils;

import jakarta.mail.MessagingException;

/**
 * This class tests whether the event handler methods defined in MessageHandler
 * are called at the appropriate times and in good order.
 */
public class MessageHandlerTest {

	@Mock
	private MessageHandlerFactory messageHandlerFactory;

	@Mock
	private MessageHandler messageHandler;

	@Mock
	private MessageHandler messageHandler2;

	private SMTPServer smtpServer;

	@Before
	public void setup() {

		EasyMockSupport.injectMocks(this);

		smtpServer = new SMTPServer(messageHandlerFactory);
		smtpServer.setPort(2566);
		smtpServer.start();
	}

	@Test
	public void testCompletedMailTransaction() throws Exception {

		EasyMock.expect(messageHandlerFactory.create(EasyMock.anyObject(MessageContext.class)))
				.andReturn(messageHandler);

		messageHandler.from(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler.recipient(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler.data(EasyMock.anyObject(InputStream.class));
		EasyMock.expectLastCall();

		messageHandler.done();
		EasyMock.expectLastCall();

		EasyMock.replay(messageHandler, messageHandler2, messageHandlerFactory);

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");
		client.from("john@example.com");
		client.to("jane@example.com");
		client.dataStart();
		client.dataWrite(TextUtils.getAsciiBytes("body"), 4);
		client.dataEnd();
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		EasyMock.verify(messageHandler, messageHandler2, messageHandlerFactory);
	}

	@Test
	public void testDisconnectImmediately() throws Exception {

		EasyMock.replay(messageHandler, messageHandler2, messageHandlerFactory);

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		EasyMock.verify(messageHandler, messageHandler2, messageHandlerFactory);
	}

	@Test
	public void testAbortedMailTransaction() throws Exception {

		EasyMock.expect(messageHandlerFactory.create(EasyMock.anyObject(MessageContext.class)))
				.andReturn(messageHandler);

		messageHandler.from(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler.done();
		EasyMock.expectLastCall();

		EasyMock.replay(messageHandler, messageHandler2, messageHandlerFactory);

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");
		client.from("john@example.com");
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		EasyMock.verify(messageHandler, messageHandler2, messageHandlerFactory);
	}

	@Test
	public void testTwoMailsInOneSession() throws Exception {

		EasyMock.expect(messageHandlerFactory.create(EasyMock.anyObject(MessageContext.class)))
				.andReturn(messageHandler);

		messageHandler.from(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler.recipient(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler.data(EasyMock.anyObject(InputStream.class));
		EasyMock.expectLastCall();

		messageHandler.done();
		EasyMock.expectLastCall();

		EasyMock.expect(messageHandlerFactory.create(EasyMock.anyObject(MessageContext.class)))
				.andReturn(messageHandler2);

		messageHandler2.from(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler2.recipient(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler2.data(EasyMock.anyObject(InputStream.class));
		EasyMock.expectLastCall();

		messageHandler2.done();
		EasyMock.expectLastCall();

		EasyMock.replay(messageHandler, messageHandler2, messageHandlerFactory);

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");

		client.from("john1@example.com");
		client.to("jane1@example.com");
		client.dataStart();
		client.dataWrite(TextUtils.getAsciiBytes("body1"), 5);
		client.dataEnd();

		client.from("john2@example.com");
		client.to("jane2@example.com");
		client.dataStart();
		client.dataWrite(TextUtils.getAsciiBytes("body2"), 5);
		client.dataEnd();

		client.quit();

		smtpServer.stop(); // wait for the server to catch up

		EasyMock.verify(messageHandler, messageHandler2, messageHandlerFactory);
	}

	/**
	 * Test for issue 56: rejecting a Mail From causes IllegalStateException in the
	 * next Mail From attempt.
	 * 
	 * @see <a href=http://code.google.com/p/subethasmtp/issues/detail?id=56>Issue
	 *      56</a>
	 */
	@Test
	public void testMailFromRejectedFirst() throws IOException, MessagingException {

		EasyMock.expect(messageHandlerFactory.create(EasyMock.anyObject(MessageContext.class)))
				.andReturn(messageHandler);

		messageHandler.from(EasyMock.anyString());
		EasyMock.expectLastCall().andThrow(new RejectException("Test MAIL FROM rejection"));

		messageHandler.done();
		EasyMock.expectLastCall();

		EasyMock.expect(messageHandlerFactory.create(EasyMock.anyObject(MessageContext.class)))
				.andReturn(messageHandler2);

		messageHandler2.from(EasyMock.anyString());
		EasyMock.expectLastCall();

		messageHandler2.done();
		EasyMock.expectLastCall();

		EasyMock.replay(messageHandler, messageHandler2, messageHandlerFactory);

		SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");

		boolean expectedRejectReceived = false;
		try {
			client.from("john1@example.com");
		} catch (SMTPException e) {
			expectedRejectReceived = true;
		}
		Assert.assertTrue(expectedRejectReceived);

		client.from("john2@example.com");
		client.quit();

		smtpServer.stop(); // wait for the server to catch up

		EasyMock.verify(messageHandler, messageHandler2, messageHandlerFactory);
	}

}
