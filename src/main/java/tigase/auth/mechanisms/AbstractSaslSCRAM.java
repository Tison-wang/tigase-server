/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.auth.mechanisms;

import tigase.auth.SaslInvalidLoginExcepion;
import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.callbacks.*;
import tigase.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractSaslSCRAM
		extends AbstractSasl {

	public static final String TLS_UNIQUE_ID_KEY = "TLS_UNIQUE_ID_KEY";
	public static final String LOCAL_CERTIFICATE_KEY = "LOCAL_CERTIFICATE_KEY";
	protected final static byte[] DEFAULT_CLIENT_KEY = "Client Key".getBytes();
	protected final static byte[] DEFAULT_SERVER_KEY = "Server Key".getBytes();
	private final static String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final Charset CHARSET = Charset.forName("UTF-8");
	private final static Pattern CLIENT_FIRST_MESSAGE = Pattern.compile(
			"^(?<gs2Header>(?:y|n|p=(?<cbName>[a-zA-z0-9.-]+))," +
					"(?:a=(?<authzid>(?:[\\x21-\\x2B\\x2D-\\x7E]|=2C|=3D)+))?,)(?<clientFirstBare>(?<mext>m=[^\\000=]+,)" +
					"?n=(?<username>(?:[\\x21-\\x2B\\x2D-\\x7E]|=2C|=3D)+),r=(?<nonce>[\\x21-\\x2B\\x2D-\\x7E]+)(?:,.*)?)$");
	private final static Pattern CLIENT_LAST_MESSAGE = Pattern.compile("^(?<withoutProof>c=(?<cb>[a-zA-Z0-9/+=]+)," +
																			   "(?:r=(?<nonce>[\\x21-\\x2B\\x2D-\\x7E]+))(?:,.*)?),p=(?<proof>[a-zA-Z0-9/+=]+)$");

	public enum BindType {
		/**
		 * Client doesn't support channel binding.
		 */
		n,
		/**
		 * Client does support channel binding but thinks the server does not.
		 */
		y,
		/**
		 * Client requires channel binding: <code>tls-unique</code>.
		 */
		tls_unique,
		/**
		 * Client requires channel binding: <code>tls-server-end-point</code>.
		 */
		tls_server_end_point
	}

	private final String algorithm;
	private final byte[] clientKeyData;
	private final String hmacAlgorithm;
	private final String mechanismName;
	private final byte[] serverKeyData;
	private final String serverNonce;
	private byte[] bindingData = null;
	private String cfmAuthzid;
	private String cfmBareMessage;
	private String cfmCbname;
	private String cfmGs2header;
	private String cfmUsername;
	private byte[] clientKey;
	private Random random = new SecureRandom();
	private BindType requestedBindType;
	private byte[] saltedPassword;
	private String sfmMessage;
	private String sfmNonce;
	private Step step = Step.clientFirstMessage;
	private byte[] storedKey;

	public static byte[] hi(String algorithm, byte[] password, final byte[] salt, final int iterations)
			throws InvalidKeyException, NoSuchAlgorithmException {
		if (algorithm.startsWith("SHA-")) {
			algorithm = algorithm.replace("SHA-", "SHA");
		}
		final SecretKeySpec k = new SecretKeySpec(password, "Hmac" + algorithm);

		byte[] z = new byte[salt.length + 4];
		System.arraycopy(salt, 0, z, 0, salt.length);
		System.arraycopy(new byte[]{0, 0, 0, 1}, 0, z, salt.length, 4);

		byte[] u = hmac(k, z);
		byte[] result = new byte[u.length];
		System.arraycopy(u, 0, result, 0, result.length);

		int i = 1;
		while (i < iterations) {
			u = hmac(k, u);
			for (int j = 0; j < u.length; j++) {
				result[j] ^= u[j];
			}
			++i;
		}

		return result;
	}

	protected static byte[] hmac(final SecretKey key, byte[] data)
			throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance(key.getAlgorithm());
		mac.init(key);
		return mac.doFinal(data);
	}

	public static byte[] normalize(String str) {
		return str.getBytes(CHARSET);
	}

	protected AbstractSaslSCRAM(String mechanismName, String algorithm, byte[] clientKey, byte[] serverKey,
								Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
		this.mechanismName = mechanismName;
		this.algorithm = algorithm;
		this.hmacAlgorithm = "Hmac" + (algorithm.startsWith("SHA-") ? algorithm.replace("SHA-", "SHA") : algorithm);
		this.clientKeyData = clientKey;
		this.serverKeyData = serverKey;
		serverNonce = randomString();
	}

	protected AbstractSaslSCRAM(String mechanismName, String algorithm, byte[] clientKey, byte[] serverKey,
								Map<? super String, ?> props, CallbackHandler callbackHandler, String serverOnce) {
		super(props, callbackHandler);
		this.mechanismName = mechanismName;
		this.algorithm = algorithm;
		this.hmacAlgorithm = "Hmac" + (algorithm.startsWith("SHA-") ? algorithm.replace("SHA-", "SHA") : algorithm);
		this.clientKeyData = clientKey;
		this.serverKeyData = serverKey;
		this.serverNonce = serverOnce;
	}

	protected byte[] calculateC() {
		try {
			final ByteArrayOutputStream result = new ByteArrayOutputStream();

			result.write(this.cfmGs2header.getBytes());

			if (this.requestedBindType == BindType.tls_unique ||
					this.requestedBindType == BindType.tls_server_end_point) {
				result.write(bindingData);
			}
			return result.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract void checkRequestedBindType(BindType requestedBindType) throws SaslException;

	private BindType createBindType(final String cfmGs2header, final String cfmCbname) throws SaslException {
		final char t = cfmGs2header.charAt(0);

		if ('n' == t) {
			return BindType.n;
		} else if ('y' == t) {
			return BindType.y;
		} else if ("tls-unique".equals(cfmCbname)) {
			return BindType.tls_unique;
		} else if ("tls-server-end-point".equals(cfmCbname)) {
			return BindType.tls_server_end_point;
		} else {
			throw new SaslException("Unsupported channel binding type");
		}
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {
		try {
			switch (step) {
				case clientFirstMessage:
					return processClientFirstMessage(response);
				case clientFinalMessage:
					return processClientLastMessage(response);
				default:
					throw new SaslException(getMechanismName() + ": Server at illegal state");
			}
		} catch (SaslException e) {
			throw e;
		} catch (Exception e) {
			log.log(Level.FINEST, "Error evaluating response", e);
			throw new SaslException("SASL Failed", e);
		}
	}

	@Override
	public String getAuthorizationID() {
		return authorizedId;
	}

	@Override
	public String getMechanismName() {
		return mechanismName;
	}

	protected byte[] h(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		return digest.digest(data);
	}

	protected SecretKey key(final byte[] key) {
		return new SecretKeySpec(key, hmacAlgorithm);
	}

	protected byte[] processClientFirstMessage(byte[] data)
			throws SaslException, InvalidKeyException, NoSuchAlgorithmException {
		Matcher r = CLIENT_FIRST_MESSAGE.matcher(new String(data, CHARSET));
		if (!r.matches()) {
			throw new SaslException("Bad challenge syntax");
		}

		this.cfmGs2header = r.group("gs2Header");
		this.cfmCbname = r.group("cbName");
		this.requestedBindType = createBindType(this.cfmGs2header, this.cfmCbname);
		this.cfmAuthzid = r.group("authzid");
		this.cfmBareMessage = r.group("clientFirstBare");
		final String cfmMext = r.group("mext");
		this.cfmUsername = r.group("username");
		final String cfmNonce = r.group("nonce");

		checkRequestedBindType(requestedBindType);

		final ChannelBindingCallback cc = new ChannelBindingCallback("Channel binding data", this.requestedBindType);
		final NameCallback nc = new NameCallback("Authentication identity", cfmUsername);
		final AuthorizationIdCallback ai = new AuthorizationIdCallback("Authorization identity", this.cfmAuthzid);
		final PBKDIterationsCallback ic = new PBKDIterationsCallback("PBKD2 iterations");
		final SaltCallback sc = new SaltCallback("Salt");
		final SaltedPasswordCallback pc = new SaltedPasswordCallback("Salted password");

		handleCallbacks(nc, ai, ic, sc, pc, cc);

		if (pc.getSaltedPassword() == null) {
			throw new SaslException("Unknown user");
		}

		if (pc.getSaltedPassword().length == 0) {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "User {0} exists, but his password is empty.", cfmUsername);
			}
			throw new SaslException("Unknown user");
		}

		this.cfmAuthzid = ai.getAuthzId();
		if (this.cfmAuthzid == null) {
			this.cfmAuthzid = nc.getName();
		}

		validateBindingsData(requestedBindType, cc.getBindingData());
		this.bindingData = cc.getBindingData();

		this.sfmNonce = cfmNonce + serverNonce;

		this.saltedPassword = pc.getSaltedPassword();
		this.clientKey = hmac(key(saltedPassword), clientKeyData);
		this.storedKey = h(clientKey);

		final StringBuilder serverStringMessage = new StringBuilder();
		serverStringMessage.append("r=").append(sfmNonce).append(",");
		serverStringMessage.append("s=").append(Base64.encode(sc.getSalt())).append(",");
		serverStringMessage.append("i=").append(ic.getInterations());

		this.sfmMessage = serverStringMessage.toString();
		step = Step.clientFinalMessage;
		return sfmMessage.getBytes();
	}

	protected byte[] processClientLastMessage(byte[] data)
			throws SaslException, InvalidKeyException, NoSuchAlgorithmException {
		Matcher r = CLIENT_LAST_MESSAGE.matcher(new String(data, CHARSET));
		if (!r.matches()) {
			throw new SaslException("Bad challenge syntax");
		}

		final String clmWithoutProof = r.group("withoutProof");
		final byte[] clmCb = Base64.decode(r.group("cb"));
		final String clmNonce = r.group("nonce");
		final String clmProof = r.group("proof");

		byte[] calculatedCb = calculateC();
		if (!(new String(clmCb, CHARSET)).startsWith(cfmGs2header)) {
			throw new XmppSaslException(SaslError.not_authorized, "Invalid GS2 header");
		} else if (!Arrays.equals(clmCb, calculatedCb)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Channel bindings does not match. expected: {0}; received: {1}",
						new Object[]{calculatedCb, clmCb});
			}
			throw new SaslInvalidLoginExcepion(SaslError.not_authorized, cfmAuthzid, "Channel bindings does not match");
		}

		if (!clmNonce.equals(sfmNonce)) {
			throw new SaslInvalidLoginExcepion(SaslError.not_authorized, cfmAuthzid, "Wrong nonce");
		}

		final String authMessage = cfmBareMessage + "," + sfmMessage + "," + clmWithoutProof;
		byte[] clientSignature = hmac(key(storedKey), authMessage.getBytes());
		byte[] clientProof = xor(clientKey, clientSignature);

		byte[] dcp = Base64.decode(clmProof);
		boolean proofMatch = Arrays.equals(clientProof, dcp);

		if (proofMatch == false) {
			throw new SaslInvalidLoginExcepion(SaslError.not_authorized, cfmAuthzid, PASSWORD_NOT_VERIFIED_MSG);
		}

		final AuthorizeCallback ac = new AuthorizeCallback(cfmUsername, cfmAuthzid);
		handleCallbacks(ac);
		if (ac.isAuthorized() == true) {
			authorizedId = ac.getAuthorizedID();
		} else {
			throw new SaslInvalidLoginExcepion(SaslError.invalid_authzid, cfmAuthzid,
											   "SCRAM: " + cfmAuthzid + " is not authorized to act as " + cfmAuthzid);
		}

		byte[] serverKey = hmac(key(saltedPassword), serverKeyData);
		byte[] serverSignature = hmac(key(serverKey), authMessage.getBytes());

		final StringBuilder serverStringMessage = new StringBuilder();
		serverStringMessage.append("v=").append(Base64.encode(serverSignature));
		step = Step.finished;
		complete = true;
		return serverStringMessage.toString().getBytes();
	}

	private String randomString() {
		final int length = 20;
		final int x = ALPHABET.length();
		char[] buffer = new char[length];
		for (int i = 0; i < length; i++) {
			int r = random.nextInt(x);
			buffer[i] = ALPHABET.charAt(r);
		}
		return new String(buffer);
	}

	@Override
	public byte[] unwrap(byte[] incoming, int offset, int len) {
		return null;
	}

	protected void validateBindingsData(BindType requestedBindType, byte[] bindingData) {
		if (requestedBindType == BindType.tls_server_end_point && bindingData == null) {
			throw new RuntimeException("Binding data not found!");
		} else if (requestedBindType == BindType.tls_unique && bindingData == null) {
			throw new RuntimeException("Binding data not found!");
		}
	}

	@Override
	public byte[] wrap(byte[] outgoing, int offset, int len) {
		return null;
	}

	protected byte[] xor(final byte[] a, final byte[] b) {
		final int l = a.length;
		byte[] r = new byte[l];
		for (int i = 0; i < l; i++) {
			r[i] = (byte) (a[i] ^ b[i]);
		}
		return r;
	}

	private enum Step {
		clientFinalMessage,
		clientFirstMessage,
		finished
	}

}
