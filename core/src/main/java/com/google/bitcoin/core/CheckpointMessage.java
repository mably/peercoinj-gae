package com.google.bitcoin.core;

public class CheckpointMessage extends Message {
	public CheckpointMessage(NetworkParameters params, byte[] msg) throws ProtocolException {
		super(params, msg, 0);
	}
	@Override
	void parse() throws ProtocolException {
		length = bytes.length;
	}

	@Override
	protected void parseLite() throws ProtocolException {
		// TODO Auto-generated method stub

	}

}
