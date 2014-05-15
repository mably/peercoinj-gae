package net.peercoin.playground.hd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.common.io.Files;

public class XKeyFile implements XKey {
	private static final Logger log = LoggerFactory.getLogger(XKeyFile.class);
	File file;
	DeterministicKey key;
	int counter, archived;

	FileChannel lockChannel;

	@SuppressWarnings("resource")
	public XKeyFile(File file, DeterministicKey key, @Nullable DeterministicKey parent)
			throws IOException {
		this.file = file;
		File lockFile = new File(file.getParentFile(), file.getName() + ".lock");
		this.lockChannel = new FileOutputStream(lockFile).getChannel();
		log.debug("lock {}", lockFile);
		lockChannel.lock();
		try {
			if (key == null) {
				byte[] buff = new byte[1024];

				FileInputStream fis = new FileInputStream(file);
				int r, off;
				for (r = 0, off = 0; r != -1 && off < buff.length; off += r > 0 ? r : 0) {
					r = fis.read(buff, off, buff.length - off);
				}
				String str = new String(buff, 0, off).trim();
				String[] parts = str.split(",");
				key = DeterministicKey.deserializeB58(null, parts[0]);
				if (parts.length > 1)
					counter = Integer.parseInt(parts[1]);
				if (parts.length > 2)
					archived = Integer.parseInt(parts[2]);
				if (archived > counter)
					throw new ExceptionInInitializerError("archived > counter");
			} else {

			}
			this.key = key;
		} catch (Exception e) {
			close();
			throw new ExceptionInInitializerError(e);
		}
	}

	@Override
	synchronized public ECKey getNew() throws IOException {
		ECKey newKey = HDKeyDerivation.deriveChildKey(key,
				new ChildNumber(counter, !key.isPubKeyOnly()));
		counter++;
		try {
			save();
		} catch (IOException e) {
			counter--;
			throw e;
		}
		return newKey;
	}

	synchronized public List<ECKey> getAll() {
		List<ECKey> out = new ArrayList<ECKey>();
		for (int i = archived; i < counter; i++)
			out.add(key.derive(i));
		return out;
	}

	@Override
	public int getCounter() {
		return counter;
	}

	@Override
	public int getArchived() {
		return archived;
	}

	@Override
	synchronized public void setArchived(int archived) throws IOException {
		if (archived == this.archived)
			return;
		if (archived > counter)
			throw new IllegalArgumentException("archived > counter");
		if (archived < this.archived)
			throw new IllegalArgumentException("archived < currently archived");
		int old = this.archived;
		this.archived = archived;
		try {
			save();
		} catch (IOException e) {
			this.archived = old;
			throw e;
		}
	}

	public File getFile() {
		return file;
	}

	@Override
	public DeterministicKey getKey() {
		return key;
	}

	@Override
	synchronized public ECKey getKey(int seq) {
		if (seq >= counter)
			throw new IllegalArgumentException("seq >= counter");
		return HDKeyDerivation.deriveChildKey(key, new ChildNumber(seq, !key.isPubKeyOnly()));
	}

	void save() throws IOException {
		File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
		PrintStream ps = new PrintStream(tmp);
		ps.println((key.hasPrivKey() ? key.serializePrivB58() : key.serializePubB58()) + ","
				+ counter + "," + archived);
		ps.flush();
		ps.close();
		Files.copy(tmp, file);
	}

	synchronized public void close() {
		key = null;
		file = null;
		counter = -1;
		archived = -1;
		try {
			lockChannel.close();
		} catch (IOException e) {
			throw new RuntimeException("lockChannel closing exception", e);
		} finally {
			lockChannel = null;
		}
	}

	@Override
	public ECKey peekNewKey() {
		return key.derive(counter);
	}

	@Override
	public XKey child(int i) {
		File f = new File(file.getParentFile(), file.getName() + "." + i);
		try {
			return new XKeyFile(f, f.exists() ? null : key.derive(i), key);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
