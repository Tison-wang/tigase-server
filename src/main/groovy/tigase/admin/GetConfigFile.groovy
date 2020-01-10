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
/*

Get configuration file

AS:Description: Get configuration file
AS:CommandId: get-config-file
AS:Component: message-router
AS:Group: Configuration
*/

package tigase.admin

import tigase.component.DSLBeanConfigurator
import tigase.kernel.core.Kernel
import tigase.server.Command
import tigase.server.Packet

def p = (Packet) packet
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def CFGFILE_TYPE = "config-file";
def CFGFILE_OPTIONS = [ "config.tdsl", "tigase.conf" ];

def cfgfile = Command.getFieldValue(p, CFGFILE_TYPE);

def result = p.commandResult(cfgfile ? Command.DataType.result : Command.DataType.form);

if (!isServiceAdmin) {
	Command.addTextField(result, "Error", "You are not service administrator");
} else if (cfgfile == null) {
	def filesArray = CFGFILE_OPTIONS.toArray(new String[CFGFILE_OPTIONS.size()]);
	Command.addFieldValue(result, CFGFILE_TYPE, "config.tdsl", "File", filesArray, filesArray);
} else {
	def filepath = [ ]
	switch (cfgfile) {
		case "config.tdsl":
			filepath = [ ((Kernel) kernel).getInstance(DSLBeanConfigurator).
								 getConfigHolder().
								 getConfigFilePath().
								 toString() ];
			break;

		case "tigase.conf":
			def filenames = [ "/etc/default/tigase", "/etc/tigase/tigase.conf", "etc/tigase.conf" ];
			filenames.each { it ->
				def file = new File(it);
				if (filepath.size() == 0 && file.exists()) {
					filepath.add(it);
				}
			};
			break;

		default:
			break;
	}

	if (filepath == null) {
		Command.addTextField(result, "Error", "Config file not specified");
	} else {
		filepath.each { it ->
			def file = new File(it);
			def lines = [ ];
			file.eachLine { line -> lines += line; };
			Command.addFieldMultiValue(result, "Content", lines);
		}
	}
}

return result;
