package org.subethamail.smtp.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;

public class PlainAuthenticatorTest {

	@Mock
	private SmartClient smartClient;

	private final Map<String, String> extensions = new HashMap<String, String>();

	@Before
	public void setUp() {
		EasyMockSupport.injectMocks(this);
	}

	@Test
	public void testSuccess() throws IOException {

		extensions.put("AUTH", "GSSAPI DIGEST-MD5 PLAIN");

		SMTPClient.Response response = new SMTPClient.Response(1234, "test");

		EasyMock.expect(smartClient.getExtensions()).andReturn(extensions);

		// base 64 encoded NULL test NULL 1234
		EasyMock.expect(smartClient.sendAndCheck("AUTH PLAIN AHRlc3QAMTIzNA==")).andReturn(response);

		EasyMock.replay(smartClient);

		PlainAuthenticator authenticator = new PlainAuthenticator(smartClient, "test", "1234");
		authenticator.authenticate();

		EasyMock.verify(smartClient);
	}

}
