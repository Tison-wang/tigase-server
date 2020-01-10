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
package tigase.io;

import tigase.server.Lifecycle;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.security.KeyStore;
import java.security.cert.CertificateParsingException;
import java.util.Map;

/**
 * Describe interface SSLContextContainerIfc here.
 * <br>
 * Created: Tue Nov 20 11:43:32 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface SSLContextContainerIfc
		extends Lifecycle {

	/**
	 * Constant <code>ALLOW_INVALID_CERTS_KEY</code> is a key pointing to a configuration parameters specyfying if
	 * invalid certificates are acceptable by the server. Invalid certificates are expired ones or certificates issued
	 * for a different domain. This should be really set to <code>false</code> in any real deployment and can be set ot
	 * <code>true</code> in development invironment.
	 */
	String ALLOW_INVALID_CERTS_KEY = "allow-invalid-certs";

	/**
	 * Constant <code>ALLOW_INVALID_CERTS_VAL</code> is a default configuration parameter specifying if invalid
	 * certificates are acceptable by the server.
	 */
	String ALLOW_INVALID_CERTS_VAL = "false";

	/**
	 * Constant <code>ALLOW_SELF_SIGNED_CERTS_KEY</code> is a key pointing to a configuration parameter specifying if
	 * self-signed certificates are acceptable for the server.
	 */
	String ALLOW_SELF_SIGNED_CERTS_KEY = "allow-self-signed-certs";

	/**
	 * Constant <code>ALLOW_SELF_SIGNED_CERTS_VAL</code> is a default configuration value specifying if self-signed
	 * certificates are allowed by the server.
	 */
	String ALLOW_SELF_SIGNED_CERTS_VAL = "true";

	String CERT_ALIAS_KEY = "cert-alias";

	String CERT_SAVE_TO_DISK_KEY = "cert-save-to-disk";

	/**
	 * Constant <code>DEFAULT_DOMAIN_CERT_KEY</code> is a key pointing to the domain with default certificate.
	 */
	String DEFAULT_DOMAIN_CERT_KEY = "ssl-def-cert-domain";

	/**
	 * Constant <code>DEFAULT_DOMAIN_CERT_VAL</code> keeps default value for a domain with default certificate.
	 */
	String DEFAULT_DOMAIN_CERT_VAL = "default";

	/**
	 * Constant <code>JKS_KEYSTORE_FILE_KEY</code> is a key pointing to a JKS keystore file.
	 */
	String JKS_KEYSTORE_FILE_KEY = "keys-store";

	/**
	 * Constant <code>JKS_KEYSTORE_FILE_VAL</code> keeps default value for a JKS keystore file.
	 */
	String JKS_KEYSTORE_FILE_VAL = "certs" + File.separator + "rsa-keystore";

	/**
	 * Constant <code>JKS_KEYSTORE_PWD_KEY</code> is a key pointing to a private key password,
	 */
	String JKS_KEYSTORE_PWD_KEY = "keys-store-password";

	/**
	 * Constant <code>JKS_KEYSTORE_PWD_VAL</code> is a default private key password.
	 */
	String JKS_KEYSTORE_PWD_VAL = "keystore";

	String PEM_CERTIFICATE_KEY = "pem-certificate";

	/**
	 * Constant <code>SERVER_CERTS_DIR_KEY</code> is a key pointing to a configuration parameter with directory names
	 * where all server certificates are stored. This can be a comma separated list of directories, instead of a single
	 * directory name. Certificates are stored in <code>*.pem</code> files where the first part of the file name is a
	 * domain name i.e.: <code>yourdomain.com.pem</code>. There is one exception though. The file named
	 * <code>default.pem</code> stores a certificate which is a default certificate for the server if certificate for
	 * specific domain is missing.
	 */
	String SERVER_CERTS_LOCATION_KEY = "ssl-certs-location";

	/**
	 * Constant <code>SERVER_CERTS_DIR_VAL</code> is a default directory name where all certificate files are stored.
	 */
	String SERVER_CERTS_LOCATION_VAL = "certs/";

	/**
	 * Constant <code>SSL_CONTAINER_CLASS_KEY</code> is a key pointing to a container implementation class. The class is
	 * loaded at startup time and initialized using configuration parameters. Some container implementations may accept
	 * different parameters set. Please refer to the implementation for more details.
	 */
	String SSL_CONTAINER_CLASS_KEY = "ssl-container-class";

	/**
	 * Constant <code>SSL_CONTAINER_CLASS_VAL</code> keeps default container implementation class loaded if none is
	 * specified in configuration file.
	 */
	String SSL_CONTAINER_CLASS_VAL = SSLContextContainer.class.getName();

	/**
	 * Constant <code>TRUSTED_CERTS_DIR_KEY</code> is a key pointing to a configuration parameter where all trusted
	 * certificates are stored. This can be a comma separated list of directories.
	 */
	String TRUSTED_CERTS_DIR_KEY = "trusted-certs-dir";

	/**
	 * Constant <code>TRUSTED_CERTS_DIR_VAL</code> is a default directory name where all trusted certificates are
	 * stored.
	 */
	String TRUSTED_CERTS_DIR_VAL = "/etc/ssl/certs";

	/**
	 * Constant <code>TRUSTSTORE_FILE_KEY</code> is a key pointing to a trust store file.
	 */
	String TRUSTSTORE_FILE_KEY = "trusts-store";

	/**
	 * Constant <code>TRUSTSTORE_FILE_VAL</code> is a default truststore file.
	 */
	String TRUSTSTORE_FILE_VAL = "certs" + File.separator + "truststore";

	/**
	 * Constant <code>TRUSTSTORE_PWD_KEY</code> is a key pointing to a trustore file password.
	 */
	String TRUSTSTORE_PWD_KEY = "trusts-store-password";

	/**
	 * Constant <code>TRUSTSTORE_PWD_VAL</code> is a default password for truststore file.
	 */
	String TRUSTSTORE_PWD_VAL = "truststore";

	// ~--- methods
	// --------------------------------------------------------------

	/**
	 * Method <code>addCertificates</code> allows to add more certificates at run time after the container has bee
	 * already initialized. This is to avoid server restart if there are certificates updates or new certificates for
	 * new virtual domain. The method should add new certificates or replace existing one if there is already a
	 * certificate for a domain.
	 *
	 * @param params a <code>Map</code> value with configuration parameters.
	 *
	 * @throws CertificateParsingException
	 */
	void addCertificates(Map<String, String> params) throws CertificateParsingException;

	IOInterface createIoInterface(String protocol, String tls_hostname, int port, boolean clientMode,
								  boolean wantClientAuth, boolean needClientAuth, ByteOrder byteOrder,
								  TrustManager[] x509TrustManagers, TLSEventHandler eventHandler, IOInterface ioi,
								  CertificateContainerIfc certificateContainer) throws IOException;

	// ~--- get methods
	// ----------------------------------------------------------

	/**
	 * Method <code>getSSLContext</code> creates and returns new SSLContext for a given domain (hostname). For creation
	 * of the SSLContext a certificate associated with this domain (hostname) should be used. If there is no specific
	 * certificate for a given domain then default certificate should be used.
	 *
	 * @param protocol a <code>String</code> is either 'SSL' or 'TLS' value.
	 * @param hostname a <code>String</code> value keeps a hostname or domain for SSLContext.
	 * @param clientMode if set SSLContext will be created for client mode (ie. creation of server certificate will be
	 * skipped if there is no certificate)
	 *
	 * @return a <code>SSLContext</code> value
	 */
	SSLContext getSSLContext(String protocol, String hostname, boolean clientMode);

	/**
	 * Method <code>getSSLContext</code> creates and returns new SSLContext for a given domain (hostname). For creation
	 * of the SSLContext a certificate associated with this domain (hostname) should be used. If there is no specific
	 * certificate for a given domain then default certificate should be used.
	 *
	 * @param protocol a <code>String</code> is either 'SSL' or 'TLS' value.
	 * @param hostname a <code>String</code> value keeps a hostname or domain for SSLContext.
	 * @param clientMode if set SSLContext will be created for client mode (ie. creation of server certificate will be
	 * skipped if there is no certificate)
	 * @param tms array of TrustManagers which should be used to validate remote certificate
	 *
	 * @return a <code>SSLContext</code> value
	 */
	SSLContext getSSLContext(String protocol, String hostname, boolean clientMode, TrustManager[] tms);

	/**
	 * Returns a trust store with all trusted certificates.
	 *
	 * @return a KeyStore with all trusted certificates, the KeyStore can be empty but cannot be null.
	 */
	KeyStore getTrustStore();

	String[] getEnabledCiphers();

	String[] getEnabledProtocols();
}

