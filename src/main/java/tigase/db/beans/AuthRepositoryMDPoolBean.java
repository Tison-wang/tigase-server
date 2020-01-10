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
package tigase.db.beans;

import tigase.auth.CredentialsDecoderBean;
import tigase.auth.CredentialsEncoderBean;
import tigase.component.exceptions.RepositoryException;
import tigase.db.AuthRepository;
import tigase.db.AuthRepositoryMDImpl;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.BasicComponent;
import tigase.stats.StatisticsCollector;
import tigase.stats.StatisticsList;

/**
 * Class implements bean for multi domain pool for authentication repositories.
 * <br>
 * Created by andrzej on 08.03.2016.
 */
@Bean(name = "authRepository", parent = Kernel.class, exportable = true, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class AuthRepositoryMDPoolBean
		extends AuthRepositoryMDImpl {

	@Override
	public boolean belongsTo(Class<? extends BasicComponent> component) {
		return StatisticsCollector.class.isAssignableFrom(component);
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		super.getStatistics(getName(), list);
	}

	@Override
	public Class<? extends AuthRepositoryConfigBean> getConfigClass() {
		return AuthRepositoryConfigBean.class;
	}

	@Override
	public Class<?> getDefaultBeanClass() {
		return AuthRepositoryConfigBean.class;
	}

	public static class AuthRepositoryConfigBean
			extends AuthUserRepositoryConfigBean<AuthRepository, AuthRepositoryConfigBean> {

		@Inject
		private CredentialsDecoderBean credentialsDecoderBean;
		@Inject
		private CredentialsEncoderBean credentialsEncoderBean;

		@Override
		protected Class<AuthRepository> getRepositoryIfc() {
			return AuthRepository.class;
		}

		@Override
		protected String getRepositoryPoolClassName() {
			return null;
		}

		@Override
		protected void initRepository(AuthRepository repository) throws RepositoryException {
			super.initRepository(repository);
			repository.setCredentialsCodecs(credentialsEncoderBean, credentialsDecoderBean);
		}
	}
}
