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
package tigase.db.jdbc;

import tigase.auth.mechanisms.AbstractSaslSCRAM;
import tigase.db.*;
import tigase.db.util.RepositoryVersionAware;
import tigase.util.Base64;
import tigase.xmpp.jid.BareJID;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tigase Salted Password Auth.
 */
@Repository.SchemaId(id = Schema.SERVER_SCHEMA_ID, name = Schema.SERVER_SCHEMA_NAME)
public class TigaseSPAuth
		extends TigaseCustomAuth
		implements RepositoryVersionAware {

	private static final Logger log = Logger.getLogger(TigaseSPAuth.class.getName());

	private static final SecureRandom random = new SecureRandom();

	private static final String encode(String pwd) throws InvalidKeyException, NoSuchAlgorithmException {
		byte[] salt = new byte[20];
		random.nextBytes(salt);
		return encode(pwd, salt);
	}

	private static final String encode(final String pwd, final byte[] salt)
			throws InvalidKeyException, NoSuchAlgorithmException {
		byte[] saltedPassword = AbstractSaslSCRAM.hi("SHA1", AbstractSaslSCRAM.normalize(pwd), salt, 4096);
		byte[] result = new byte[salt.length + saltedPassword.length];

		System.arraycopy(salt, 0, result, 0, salt.length);
		System.arraycopy(saltedPassword, 0, result, salt.length, saltedPassword.length);

		return Base64.encode(result);
	}

	@Override
	public void addUser(BareJID user, String password) throws UserExistsException, TigaseDBException {
		try {
			super.addUser(user, encode(password));
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't add user " + user, e);
		}
	}

	@Override
	public boolean otherAuth(Map<String, Object> props)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String password = (String) props.get(PASSWORD_KEY);
		BareJID user_id = (BareJID) props.get(USER_ID_KEY);

		try {
			props.put(PASSWORD_KEY, encodeWithUserSalt(user_id, password));
			return super.otherAuth(props);
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't salt user password", e);
			throw new AuthorizationException("Can't salt user password", e);
		}
	}

	@Override
	public void updatePassword(BareJID user, String password) throws UserNotFoundException, TigaseDBException {
		try {
			super.updatePassword(user, encode(password));
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't update password for user " + user, e);
		}
	}

	private String encodeWithUserSalt(final BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException, InvalidKeyException, NoSuchAlgorithmException {
		String pwd = getPassword(user);
		if (pwd == null) {
			throw new UserNotFoundException("User " + user + " not found.");
		}
		byte[] buffer = Base64.decode(pwd);
		byte[] salt = new byte[20];
		System.arraycopy(buffer, 0, salt, 0, salt.length);

		return encode(password, salt);
	}

	@SuppressWarnings("unused")
	private boolean isPasswordValid(final BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException, InvalidKeyException, NoSuchAlgorithmException {

		// String saltedPassword = getPassword(user);
		String pwd = getPassword(user);
		if (pwd == null) {
			throw new UserNotFoundException("User " + user + " not found.");
		}
		byte[] buffer = Base64.decode(pwd);
		byte[] salt = new byte[20];
		byte[] saltedPassword = new byte[20];
		System.arraycopy(buffer, 0, salt, 0, salt.length);
		System.arraycopy(buffer, salt.length, saltedPassword, 0, saltedPassword.length);

		byte[] np = AbstractSaslSCRAM.hi("SHA1", AbstractSaslSCRAM.normalize(password), salt, 4096);

		return Arrays.equals(saltedPassword, np);
	}

}
