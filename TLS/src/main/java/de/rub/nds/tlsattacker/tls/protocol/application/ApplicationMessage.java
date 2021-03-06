/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.protocol.application;

import de.rub.nds.tlsattacker.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.tlsattacker.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.tlsattacker.tls.constants.ConnectionEnd;
import de.rub.nds.tlsattacker.tls.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.tls.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.tls.protocol.ProtocolMessageHandler;
import de.rub.nds.tlsattacker.tls.workflow.TlsContext;

/**
 * @author Juraj Somorovsky <juraj.somorovsky@rub.de>
 */
public class ApplicationMessage extends ProtocolMessage {

    @ModifiableVariableProperty
    ModifiableByteArray data;

    public ApplicationMessage() {
	this.protocolMessageType = ProtocolMessageType.APPLICATION_DATA;
    }

    public ApplicationMessage(ConnectionEnd messageIssuer) {
	this();
	this.messageIssuer = messageIssuer;
    }

    public ModifiableByteArray getData() {
	return data;
    }

    public void setData(ModifiableByteArray data) {
	this.data = data;
    }

    public void setData(byte[] data) {
	if (this.data == null) {
	    this.data = new ModifiableByteArray();
	}
	this.data.setOriginalValue(data);
    }

    @Override
    public ProtocolMessageHandler<? extends ProtocolMessage> getProtocolMessageHandler(TlsContext tlsContext) {
	ApplicationHandler ah = new ApplicationHandler(tlsContext);
	ah.setProtocolMessage(this);
	return ah;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("\nApplication Data:");
	return sb.toString();
    }

    @Override
    public String toCompactString() {
	return "Application";
    }
}
