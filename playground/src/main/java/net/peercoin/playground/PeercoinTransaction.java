package net.peercoin.playground;

import static com.google.bitcoin.core.Utils.uint32ToByteStreamLE;

import java.io.IOException;
import java.io.OutputStream;

import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.VarInt;

public class PeercoinTransaction extends Transaction {

	public PeercoinTransaction(NetworkParameters params, byte[] msg,
			int offset, Message parent, boolean parseLazy, boolean parseRetain,
			int length) throws ProtocolException {
		super(params, msg, offset, parent, parseLazy, parseRetain, length);
		// TODO Auto-generated constructor stub
	}

	public PeercoinTransaction(NetworkParameters params, byte[] payload,
			int offset) throws ProtocolException {
		super(params, payload, offset);
		// TODO Auto-generated constructor stub
	}

	public PeercoinTransaction(NetworkParameters params, byte[] msg,
			Message parent, boolean parseLazy, boolean parseRetain, int length)
			throws ProtocolException {
		super(params, msg, parent, parseLazy, parseRetain, length);
		// TODO Auto-generated constructor stub
	}

	public PeercoinTransaction(NetworkParameters params, byte[] payloadBytes)
			throws ProtocolException {
		super(params, payloadBytes);
		// TODO Auto-generated constructor stub
	}

	public PeercoinTransaction(NetworkParameters params, int version,
			Sha256Hash hash) {
		super(params, version, hash);
		// TODO Auto-generated constructor stub
	}

	public PeercoinTransaction(NetworkParameters params) {
		super(params);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void bitcoinSerializeToStream(OutputStream stream)
			throws IOException {
		uint32ToByteStreamLE(version, stream);
		uint32ToByteStreamLE(time, stream);
		stream.write(new VarInt(inputs.size()).encode());
		for (TransactionInput in : inputs)
			in.bitcoinSerialize(stream);
		stream.write(new VarInt(outputs.size()).encode());
		for (TransactionOutput out : outputs)
			out.bitcoinSerialize(stream);
		uint32ToByteStreamLE(lockTime, stream);
	}
}
