package net.peercoin.playground.trash.spongy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.generators.SCrypt;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.Arrays;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Utils;

class ExtendedKey {
	public static class ValidationException extends Exception {

		public ValidationException(String message) {
			super(message);
			// TODO Auto-generated constructor stub
		}

		public ValidationException(Throwable cause) {
			super(cause);
			// TODO Auto-generated constructor stub
		}

	}

	private static final String SEC_PROVIDER = "SC";
	private static final SecureRandom rnd = new SecureRandom();
	private static final X9ECParameters curve = SECNamedCurves
			.getByName("secp256k1");

	private final ECKey master;
	private final byte[] chainCode;
	private final int depth;
	private final int parent;
	private final int sequence;

	private static final byte[] BITCOIN_SEED = "Bitcoin seed".getBytes();

	public static ExtendedKey createFromPassphrase(String passphrase,
			byte[] encrypted) throws ValidationException {
		try {
			byte[] key = SCrypt.generate(passphrase.getBytes("UTF-8"),
					BITCOIN_SEED, 16384, 8, 8, 32);
			SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
			if (encrypted.length == 32) {
				// asssume encrypted is seed
				Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding", SEC_PROVIDER);
				cipher.init(Cipher.DECRYPT_MODE, keyspec);
				return create(cipher.doFinal(encrypted));
			} else {
				// assume encrypted serialization of a key
				Cipher cipher = Cipher
						.getInstance("AES/CBC/PKCS5Padding", SEC_PROVIDER);
				byte[] iv = Arrays.copyOfRange(encrypted, 0, 16);
				byte[] data = Arrays.copyOfRange(encrypted, 16,
						encrypted.length);
				cipher.init(Cipher.DECRYPT_MODE, keyspec, new IvParameterSpec(
						iv));
				return ExtendedKey.parse(new String(cipher.doFinal(data)));
			}
		} catch (UnsupportedEncodingException e) {
			throw new ValidationException(e);
		} catch (IllegalBlockSizeException e) {
			throw new ValidationException(e);
		} catch (BadPaddingException e) {
			throw new ValidationException(e);
		} catch (InvalidKeyException e) {
			throw new ValidationException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new ValidationException(e);
		} catch (NoSuchProviderException e) {
			throw new ValidationException(e);
		} catch (NoSuchPaddingException e) {
			throw new ValidationException(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new ValidationException(e);
		}
	}

	public byte[] encrypt(String passphrase, boolean production)
			throws ValidationException {
		try {
			byte[] key = SCrypt.generate(passphrase.getBytes("UTF-8"),
					BITCOIN_SEED, 16384, 8, 8, 32);
			SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", SEC_PROVIDER);
			cipher.init(Cipher.ENCRYPT_MODE, keyspec);
			byte[] iv = cipher.getIV();
			byte[] c = cipher.doFinal(serialize(production).getBytes());
			byte[] result = new byte[iv.length + c.length];
			System.arraycopy(iv, 0, result, 0, iv.length);
			System.arraycopy(c, 0, result, iv.length, c.length);
			return result;
		} catch (Exception e) {
			throw new ValidationException(e);
		}
	}

	public static ExtendedKey create(byte[] seed) throws ValidationException {
		try {
			Mac mac = Mac.getInstance("HmacSHA512", SEC_PROVIDER);
			SecretKey seedkey = new SecretKeySpec(BITCOIN_SEED, "HmacSHA512");
			mac.init(seedkey);
			byte[] lr = mac.doFinal(seed);
			byte[] l = Arrays.copyOfRange(lr, 0, 32);
			byte[] r = Arrays.copyOfRange(lr, 32, 64);
			BigInteger m = new BigInteger(1, l);
			if (m.compareTo(curve.getN()) >= 0) {
				throw new ValidationException(
						"This is rather unlikely, but it did just happen");
			}
			ECKey keyPair = new ECKey(new BigInteger(1, l), null, true);
			return new ExtendedKey(keyPair, r, 0, 0, 0);
		} catch (NoSuchAlgorithmException e) {
			throw new ValidationException(e);
		} catch (NoSuchProviderException e) {
			throw new ValidationException(e);
		} catch (InvalidKeyException e) {
			throw new ValidationException(e);
		}
	}

	public static ExtendedKey createNew() {
		byte[] chainCode = new byte[32];
		rnd.nextBytes(chainCode);
		ECKey key = new ECKey(new BigInteger(1, chainCode), null, true);
		rnd.nextBytes(chainCode);
		return new ExtendedKey(key, chainCode, 0, 0, 0);
	}

	public ExtendedKey(ECKey key, byte[] chainCode, int depth, int parent,
			int sequence) {
		this.master = key;
		this.chainCode = chainCode;
		this.parent = parent;
		this.depth = depth;
		this.sequence = sequence;
	}

	public ECKey getMaster() {
		return master;
	}

	public byte[] getChainCode() {
		return Arrays.clone(chainCode);
	}

	public int getDepth() {
		return depth;
	}

	public int getParent() {
		return parent;
	}

	public int getSequence() {
		return sequence;
	}

	public int getFingerPrint() {
		int fingerprint = 0;
		byte[] address = master.getPubKeyHash();
		for (int i = 0; i < 4; ++i) {
			fingerprint <<= 8;
			fingerprint |= address[i] & 0xff;
		}
		return fingerprint;
	}

	public ECKey getKey(int sequence) throws ValidationException {
		return generateKey(sequence).getMaster();
	}

	public ExtendedKey getChild(int sequence) throws ValidationException {
		ExtendedKey sub = generateKey(sequence);
		return new ExtendedKey(sub.getMaster(), sub.getChainCode(),
				sub.getDepth() + 1, getFingerPrint(), sequence);
	}

	public ExtendedKey getReadOnly() {
		return new ExtendedKey(new ECKey(null, master.getPubKey(), true),
				chainCode, depth, parent, sequence);
	}

	public boolean isReadOnly() {
		return master.getPrivKeyBytes() == null;
	}

	private ExtendedKey generateKey(int sequence) throws ValidationException {
		try {
			if ((sequence & 0x80000000) != 0
					&& master.getPrivKeyBytes() == null) {
				throw new ValidationException(
						"need private key for private generation");
			}
			Mac mac = Mac.getInstance("HmacSHA512", SEC_PROVIDER);
			SecretKey key = new SecretKeySpec(chainCode, "HmacSHA512");
			mac.init(key);

			byte[] extended;
			byte[] pub = master.getPubKey();
			if ((sequence & 0x80000000) == 0) {
				extended = new byte[pub.length + 4];
				System.arraycopy(pub, 0, extended, 0, pub.length);
				extended[pub.length] = (byte) ((sequence >>> 24) & 0xff);
				extended[pub.length + 1] = (byte) ((sequence >>> 16) & 0xff);
				extended[pub.length + 2] = (byte) ((sequence >>> 8) & 0xff);
				extended[pub.length + 3] = (byte) (sequence & 0xff);
			} else {
				byte[] priv = master.getPrivKeyBytes();
				extended = new byte[priv.length + 5];
				System.arraycopy(priv, 0, extended, 1, priv.length);
				extended[priv.length + 1] = (byte) ((sequence >>> 24) & 0xff);
				extended[priv.length + 2] = (byte) ((sequence >>> 16) & 0xff);
				extended[priv.length + 3] = (byte) ((sequence >>> 8) & 0xff);
				extended[priv.length + 4] = (byte) (sequence & 0xff);
			}
			byte[] lr = mac.doFinal(extended);
			byte[] l = Arrays.copyOfRange(lr, 0, 32);
			byte[] r = Arrays.copyOfRange(lr, 32, 64);

			BigInteger m = new BigInteger(1, l);
			if (m.compareTo(curve.getN()) >= 0) {
				throw new ValidationException(
						"This is rather unlikely, but it did just happen");
			}
			if (master.getPrivKeyBytes() != null) {
				BigInteger k = m.add(
						new BigInteger(1, master.getPrivKeyBytes())).mod(
						curve.getN());
				if (k.equals(BigInteger.ZERO)) {
					throw new ValidationException(
							"This is rather unlikely, but it did just happen");
				}
				return new ExtendedKey(new ECKey(k, null, true), r, depth,
						parent, sequence);
			} else {
				ECPoint q = curve.getG().multiply(m)
						.add(curve.getCurve().decodePoint(pub));
				if (q.isInfinity()) {
					throw new ValidationException(
							"This is rather unlikely, but it did just happen");
				}
				pub = new ECPoint.Fp(curve.getCurve(), q.getX(), q.getY(), true)
						.getEncoded();
				return new ExtendedKey(new ECKey(null, pub, true), r, depth,
						parent, sequence);
			}
		} catch (NoSuchAlgorithmException e) {
			throw new ValidationException(e);
		} catch (NoSuchProviderException e) {
			throw new ValidationException(e);
		} catch (InvalidKeyException e) {
			throw new ValidationException(e);
		}
	}

	private static final byte[] xprv = new byte[] { 0x04, (byte) 0x88,
			(byte) 0xAD, (byte) 0xE4 };
	private static final byte[] xpub = new byte[] { 0x04, (byte) 0x88,
			(byte) 0xB2, (byte) 0x1E };
	private static final byte[] tprv = new byte[] { 0x04, (byte) 0x35,
			(byte) 0x83, (byte) 0x94 };
	private static final byte[] tpub = new byte[] { 0x04, (byte) 0x35,
			(byte) 0x87, (byte) 0xCF };

	public String serialize(boolean production) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			if (master.getPrivKeyBytes() != null) {
				if (production) {
					out.write(xprv);
				} else {
					out.write(tprv);
				}
			} else {
				if (production) {
					out.write(xpub);
				} else {
					out.write(tpub);
				}
			}
			out.write(depth & 0xff);
			out.write((parent >>> 24) & 0xff);
			out.write((parent >>> 16) & 0xff);
			out.write((parent >>> 8) & 0xff);
			out.write(parent & 0xff);
			out.write((sequence >>> 24) & 0xff);
			out.write((sequence >>> 16) & 0xff);
			out.write((sequence >>> 8) & 0xff);
			out.write(sequence & 0xff);
			out.write(chainCode);
			if (master.getPrivKeyBytes() != null) {
				out.write(0x00);
				out.write(master.getPrivKeyBytes());
			} else {
				out.write(master.getPubKey());
			}
		} catch (IOException e) {
		}
		return toBase58WithChecksum(out.toByteArray());
	}

	public static ExtendedKey parse(String serialized)
			throws ValidationException {
		byte[] data = fromBase58WithChecksum(serialized);
		if (data.length != 78) {
			throw new ValidationException("invalid extended key");
		}
		byte[] type = Arrays.copyOf(data, 4);
		boolean hasPrivate;
		if (Arrays.areEqual(type, xprv) || Arrays.areEqual(type, tprv)) {
			hasPrivate = true;
		} else if (Arrays.areEqual(type, xpub) || Arrays.areEqual(type, tpub)) {
			hasPrivate = false;
		} else {
			throw new ValidationException(
					"invalid magic number for an extended key");
		}

		int depth = data[4] & 0xff;

		int parent = data[5] & 0xff;
		parent <<= 8;
		parent |= data[6] & 0xff;
		parent <<= 8;
		parent |= data[7] & 0xff;
		parent <<= 8;
		parent |= data[8] & 0xff;

		int sequence = data[9] & 0xff;
		sequence <<= 8;
		sequence |= data[10] & 0xff;
		sequence <<= 8;
		sequence |= data[11] & 0xff;
		sequence <<= 8;
		sequence |= data[12] & 0xff;

		byte[] chainCode = Arrays.copyOfRange(data, 13, 13 + 32);
		byte[] pubOrPriv = Arrays.copyOfRange(data, 13 + 32, data.length);
		ECKey key;
		if (hasPrivate) {
			key = new ECKey(new BigInteger(1, pubOrPriv), null, true);
		} else {
			key = new ECKey(null, pubOrPriv, true);
		}
		return new ExtendedKey(key, chainCode, depth, parent, sequence);
	}

	public static String toBase58WithChecksum(byte[] bytes) {
		byte[] addressBytes = new byte[bytes.length + 4];
		System.arraycopy(bytes, 0, addressBytes, 0, bytes.length);
		byte[] check = Utils.doubleDigest(addressBytes, 0, bytes.length + 1);
		System.arraycopy(check, 0, addressBytes, bytes.length, 4);
		return Base58.encode(addressBytes);
	}

	public static byte[] fromBase58WithChecksum(String s)
			throws ValidationException {
		byte[] b;
		try {
			b = Base58.decode(s);
		} catch (AddressFormatException e) {
			throw new ValidationException(e);
		}
		if (b.length < 4) {
			throw new ValidationException("Too short for checksum " + s);
		}
		byte[] cs = new byte[4];
		System.arraycopy(b, b.length - 4, cs, 0, 4);
		byte[] data = new byte[b.length - 4];
		System.arraycopy(b, 0, data, 0, b.length - 4);
		byte[] h = new byte[4];
		System.arraycopy(Utils.doubleDigest(data), 0, h, 0, 4);
		if (Arrays.areEqual(cs, h)) {
			return data;
		}
		throw new ValidationException("Checksum mismatch " + s);
	}

}