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
Set Message of the Day
AS:Description: Set Message of the Day
AS:CommandId: http://jabber.org/protocol/admin#set-motd
AS:Component: sess-man
AS:Group: Configuration
 */
package tigase.admin

import groovy.transform.CompileStatic
import tigase.kernel.core.Kernel
import tigase.server.Command
import tigase.server.Iq
import tigase.server.Packet
import tigase.server.xmppsession.SessionManager
import tigase.xmpp.impl.MotdProcessor

Kernel kernel = (Kernel) kernel;
SessionManager component = (SessionManager) component
packet = (Iq) packet

@CompileStatic
Packet process(Kernel kernel, SessionManager component, Iq p) {
	String MOTD = "motd"

	if (!component.isAdmin(p.getStanzaFrom())) {
		def result = p.commandResult(Command.DataType.result)
		Command.addTextField(result, "Error", "You are not service administrator")
		return result
	}

	def motd = Command.getFieldValues(p, MOTD)
	if (!motd) {
		def result = p.commandResult(Command.DataType.form)

		Command.addTitle(result, "Setting the Message of the Day")
		Command.addInstructions(result, "Fill out this form to set the message of the day.")
		Command.addHiddenField(result, "FORM_TYPE", "http://jabber.org/protocol/admin")
		Command.addFieldMultiValue(result, MOTD, [ ], "Message of the Day")

		return result
	} else {
		def result = p.commandResult(Command.DataType.result)
		MotdProcessor motdProcessor = kernel.getInstance(MotdProcessor.class)
		motdProcessor.setMotd(motd.join("\n"))
		return result
	}
}

return process(kernel, component, packet)