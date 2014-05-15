package net.peercoin.playground.hd;

import java.io.IOException;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.DeterministicKey;

public interface XKey {

	public abstract ECKey getNew() throws IOException;

	public abstract int getCounter();

	public abstract int getArchived();

	public abstract void setArchived(int archived) throws IOException;

	public abstract DeterministicKey getKey();

	public abstract ECKey getKey(int seq);

	public abstract ECKey peekNewKey();

	public abstract XKey child(int i);
}