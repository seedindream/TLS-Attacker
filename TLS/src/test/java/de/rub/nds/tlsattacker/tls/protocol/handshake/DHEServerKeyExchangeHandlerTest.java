/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS.
 *
 * Copyright (C) 2015 Chair for Network and Data Security,
 *                    Ruhr University Bochum
 *                    (juraj.somorovsky@rub.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.rub.nds.tlsattacker.tls.protocol.handshake;

import de.rub.nds.tlsattacker.tls.protocol.handshake.HandshakeMessageFields;
import de.rub.nds.tlsattacker.tls.protocol.handshake.DHEServerKeyExchangeHandler;
import de.rub.nds.tlsattacker.tls.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.tls.constants.HashAlgorithm;
import de.rub.nds.tlsattacker.tls.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.tls.exceptions.ConfigurationException;
import de.rub.nds.tlsattacker.tls.protocol.handshake.CertificateMessage;
import de.rub.nds.tlsattacker.tls.protocol.handshake.DHEServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.tls.workflow.TlsContext;
import de.rub.nds.tlsattacker.util.ArrayConverter;
import de.rub.nds.tlsattacker.util.KeystoreHandler;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for ECDHE key exchange handler, with values from wireshark
 * 
 * @author Juraj Somorovsky <juraj.somorovsky@rub.de>
 * @author Philip Riese <philip.riese@rub.de>
 */
public class DHEServerKeyExchangeHandlerTest {

    static byte[] testServerKeyExchangeDSA = ArrayConverter
	    .hexStringToByteArray("0c0000b90040da583c16d9852289d0e4af756f4cca92dd4be533b804fb0fed94ef9c8a4403ed574650d3"
		    + "6999db29d776276ba2d3d412e218f4dd1e084cf6d8003e7c4774e833000102004006a14fecf0b2e7fae2b30d87961620"
		    + "7fb1022ce1000d87c3e98ede5a053799d61adc622daac01b0966232425784ffd3493f2ab3bfa109361a42c28c7ba4af7"
		    + "6c0402002e302c02144f232c10ad1fcfb92b3bedc7c0deddd5c04908ad02142211f07d891eb18a1e0d58dfba4949ffe5"
		    + "961451");

    static byte[] clientRandom = ArrayConverter
	    .hexStringToByteArray("3fddd7503dca1dd8c35d28a62c3667d77fba97f0d6c46c7e08fdb70f625edb53");

    static byte[] serverRandom = ArrayConverter
	    .hexStringToByteArray("d05579f8ae2a5862864481764db12b8af57a910debb4a706f7a3b9c664e09dd8");

    DHEServerKeyExchangeHandler handler;

    TlsContext tlsContext;

    public DHEServerKeyExchangeHandlerTest() {

	// ECC does not work properly in the NSS provider
	Security.removeProvider("SunPKCS11-NSS");
	Security.addProvider(new BouncyCastleProvider());

	tlsContext = new TlsContext();
	tlsContext.setClientRandom(clientRandom);
	tlsContext.setServerRandom(serverRandom);

	try {
	    KeyStore ks = KeystoreHandler.loadKeyStore("../resources/rsa1024.jks", "password");
	    tlsContext.setKeyStore(ks);
	    tlsContext.setAlias("alias");
	    tlsContext.setPassword("password");
	} catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException ex) {
	    throw new ConfigurationException("Something went wrong loading key from Keystore", ex);
	}
	handler = new DHEServerKeyExchangeHandler(tlsContext);
    }

    /**
     * Test of parseMessageAction method, of class DHEServerKeyExchangeHandler.
     */
    @Test
    public void testParseMessageDSA() {
	handler.initializeProtocolMessage();

	int endPointer = handler.parseMessageAction(testServerKeyExchangeDSA, 0);
	DHEServerKeyExchangeMessage message = (DHEServerKeyExchangeMessage) handler.getProtocolMessage();
	HandshakeMessageFields handshakeMessageFields = message.getMessageFields();

	assertEquals("Message type must be ServerKeyExchange", HandshakeMessageType.SERVER_KEY_EXCHANGE,
		message.getHandshakeMessageType());
	assertEquals("Message length must be 185", new Integer(185), handshakeMessageFields.getLength().getValue());
	assertEquals("p length must be 64", new Integer(64), message.getpLength().getValue());
	assertEquals("g length must be ", new Integer(1), message.getgLength().getValue());
	assertEquals("g must be 2", new BigInteger("2"), message.getG().getValue());

	assertEquals("Public key length is 64", new Integer(64), message.getPublicKeyLength().getValue());
	assertEquals("Hash must be SHA256", HashAlgorithm.SHA256,
		HashAlgorithm.getHashAlgorithm(message.getHashAlgorithm().getValue()));
	assertEquals("Signature must be DSA", SignatureAlgorithm.DSA,
		SignatureAlgorithm.getSignatureAlgorithm(message.getSignatureAlgorithm().getValue()));
	assertEquals("Signature length must be 46", new Integer(46), message.getSignatureLength().getValue());

	assertEquals("The pointer has to return the length of the protocol message", testServerKeyExchangeDSA.length,
		endPointer);
    }

    @Test
    public void testIsCorrectProtocolMessage() {
	DHEServerKeyExchangeMessage sem = new DHEServerKeyExchangeMessage();
	assertTrue(handler.isCorrectProtocolMessage(sem));

	CertificateMessage cm = new CertificateMessage();
	assertFalse(handler.isCorrectProtocolMessage(cm));
    }

    /**
     * Test of prepareMessageAction method, of class
     * DHEServerKeyExchangeHandler.
     */
    @Test
    public void testPrepareMessageRSA() {
	handler.initializeProtocolMessage();
	DHEServerKeyExchangeMessage message = (DHEServerKeyExchangeMessage) handler.getProtocolMessage();

	byte[] result = handler.prepareMessageAction();

	assertNotNull("Confirm function didn't return 'NULL'", result);
	assertEquals("Message type must be ServerKeyExchange", HandshakeMessageType.SERVER_KEY_EXCHANGE,
		message.getHandshakeMessageType());

    }
}