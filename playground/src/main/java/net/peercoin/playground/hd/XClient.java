package net.peercoin.playground.hd;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.peercoin.playground.rpc.Rpc;
import net.peercoin.playground.rpc.RpcClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;

public class XClient {
	private static final Logger log = LoggerFactory.getLogger(XClient.class);
	final Random rand;
	final XKey key, rkey, changeKey;
	final RpcClient client;
	final NetworkParameters params;

	final String label, rlabel;
	final Map<String, Integer> addrs = new HashMap<String, Integer>();
	final Set<String> raddrs = new HashSet<String>();

	String changeAddr = null, lastChangeAddr = "none";

	public XClient(XKey key, XKey rkey, RpcClient client, NetworkParameters params)
			throws IOException {
		super();

		if (key.getKey().isPubKeyOnly())
			throw new IllegalArgumentException("key is pubkey only");
		this.key = key;
		this.rkey = rkey;
		this.client = client;
		this.params = params;
		this.rand = new Random();

		this.label = "xprv_" + Base58.encode(key.getKey().getIdentifier());
		this.rlabel = rkey == null ? null : "xpub_"
				+ Base58.encode(rkey.getKey().getIdentifier()).toString();

		this.changeKey = key.child(0);

		if (this.key.getCounter() == 0)
			this.key.getNew();
		if (this.changeKey.getCounter() == 0)
			this.changeKey.getNew();
		if (this.rkey != null && this.rkey.getCounter() == 0)
			this.rkey.getNew();
	}

	synchronized public void syncClientWithKey() {
		boolean scan = false;
		boolean importNext = true;
		for (int seq = key.getCounter(); seq > 0; seq--) {
			ECKey eckey = key.getKey(seq - 1);
			String addr = eckey.toAddress(params).toString();
			addrs.put(addr, seq - 1);
			if (importNext) {
				Rpc.PubImportResult importResult = client.pubImportKey(
						Hex.toHexString(eckey.getPubKey()), label, false);
				log.debug("prv {}", importResult);
				if (!addr.equals(importResult.address))
					throw new IllegalStateException("probably different network params");
				if (!importResult.added) {
					importNext = false;
				} else
					scan = true;
			}
		}

		if (this.rkey != null) {
			importNext = true;
			for (int seq = rkey.getCounter(); seq > 0; seq--) {
				ECKey eckey = rkey.getKey(seq - 1);
				String addr = eckey.toAddress(params).toString();
				raddrs.add(addr);
				if (importNext) {
					Rpc.PubImportResult importResult = client.pubImportKey(
							Hex.toHexString(eckey.getPubKey()), rlabel, false);
					log.debug("pub {}", importResult);
					if (!addr.equals(importResult.address))
						throw new IllegalStateException("probably different network params");
					if (!importResult.added) {
						importNext = false;
					} else
						scan = true;
				}
			}
		}

		ECKey eckey = changeKey.getKey(changeKey.getCounter() - 1);
		changeAddr = eckey.toAddress(params).toString();
		Rpc.PubImportResult importResult = client.pubImportKey(Hex.toHexString(eckey.getPubKey()),
				label + "_change", false);
		log.debug("change {}", importResult);

		if (changeKey.getCounter() > 1) {
			eckey = changeKey.getKey(changeKey.getCounter() - 2);
			lastChangeAddr = eckey.toAddress(params).toString();
			importResult = client.pubImportKey(Hex.toHexString(eckey.getPubKey()), label
					+ "_change", false);
			log.debug("lastChange {}", importResult);
			if (importResult.added)
				scan = true;
		}

		if (scan)
			client.pubScan();
	}

	synchronized public ECKey newKey() throws IOException {
		ECKey key = this.key.getNew();
		String addr = key.toAddress(params).toString();
		addrs.put(addr, this.key.getCounter() - 1);
		Rpc.PubImportResult importResult = client.pubImportKey(Hex.toHexString(key.getPubKey()),
				label, false);
		if (!addr.equals(importResult.address))
			throw new IllegalStateException("probably different network params");
		if (!importResult.added)
			throw new IllegalStateException("new key isn't new: " + importResult);
		return key;
	}

	synchronized protected void newChange() throws IOException {
		ECKey key = this.changeKey.getNew();
		String addr = key.toAddress(params).toString();
		Rpc.PubImportResult importResult = client.pubImportKey(Hex.toHexString(key.getPubKey()),
				label + "_change", false);
		if (!addr.equals(importResult.address))
			throw new IllegalStateException("probably different network params");
		if (!importResult.added)
			throw new IllegalStateException("new key isn't new: " + importResult);
		this.lastChangeAddr = this.changeAddr;
		this.changeAddr = addr;
	}

	synchronized public ECKey newRKey() throws IOException {
		ECKey key = this.rkey.getNew();
		String addr = key.toAddress(params).toString();
		raddrs.add(addr);
		Rpc.PubImportResult importResult = client.pubImportKey(Hex.toHexString(key.getPubKey()),
				rlabel, false);
		if (!addr.equals(importResult.address))
			throw new IllegalStateException("probably different network params");
		if (!importResult.added)
			throw new IllegalStateException("new key isn't new: " + importResult);
		return key;
	}

	synchronized public Rpc.Unspent[] listUnpsent() {
		Rpc.Unspent[] unsp = client.listUnspent(0, 9999999);
		List<Rpc.Unspent> l = new ArrayList<Rpc.Unspent>(unsp.length);
		for (Rpc.Unspent u : unsp)
			if (lastChangeAddr.equals(u.address) || addrs.containsKey(u.address))
				l.add(u);
		return l.toArray(new Rpc.Unspent[l.size()]);
	}

	synchronized public Rpc.Unspent[] listUnpsent(String addr) {
		return client.listUnspent(0, 9999999, addr);
	}

	synchronized public Sha256Hash sendToAddress(Address address, final BigInteger amount)
			throws IOException {
		if (changeAddr == null)
			throw new IllegalStateException("no reserve key");
		if (address.getParameters().equals(params))
			throw new IllegalArgumentException("address with different params");
		BigInteger txFee = Utils.CENT;
		BigInteger inputs = BigInteger.ZERO;
		Rpc.Unspent[] unspent = listUnpsent();

		byte[] serialized = null;
		Transaction tx = null;

		for (int i = 0; i < unspent.length && serialized == null; i++) {
			BigInteger uvalue = toMicroCoins(unspent[i].amount);
			inputs = inputs.add(uvalue);
			BigInteger rest;
			while ((rest = inputs.subtract(amount).subtract(txFee)).compareTo(BigInteger.ZERO) >= 0
					&& serialized == null) {
				tx = new Transaction(params);
				tx.setTime(System.currentTimeMillis() / 1000);
				TransactionOutput[] output = new TransactionOutput[2];
				output[0] = new TransactionOutput(params, tx, amount, address);
				if (rest.compareTo(Utils.CENT) >= 0) {
					try {
						output[1] = new TransactionOutput(params, tx, rest, new Address(params,
								changeAddr));
					} catch (AddressFormatException e) {
						throw new IllegalStateException("shouldn't happen");
					}
				}
				if (output[1] == null)
					tx.addOutput(output[0]);
				else {
					boolean n = rand.nextBoolean();
					tx.addOutput(output[n ? 0 : 1]);
					tx.addOutput(output[n ? 1 : 0]);
				}

				for (int j = 0; j <= i; j++)
					tx.addInput(new TransactionInput(params, tx, null, new TransactionOutPoint(
							params, unspent[j].vout, new Sha256Hash(unspent[j].txid))));
				for (int j = 0; j <= i; j++) {
					ECKey inputKey = lastChangeAddr.equals(unspent[j].address) ? changeKey
							.getKey(changeKey.getCounter() - 2) : key.getKey(addrs
							.get(unspent[j].address));
					TransactionSignature sig = tx.calculateSignature(j, inputKey,
							new Script(Hex.decode(unspent[j].scriptPubKey)), SigHash.ALL, false);
					tx.getInput(j).setScriptSig(ScriptBuilder.createInputScript(sig, inputKey));
				}
				serialized = tx.bitcoinSerialize();
				BigInteger fee = BigInteger.valueOf(serialized.length)
						.divide(BigInteger.valueOf(1000)).add(BigInteger.ONE).multiply(Utils.CENT);
				if (fee.compareTo(txFee) > 0) {
					serialized = null;
					txFee = fee;
				}
			}
		}
		log.debug("{} {} {}", serialized != null, txFee, tx);
		if (serialized == null)
			throw new IllegalStateException("insufficient funds");
		String txId = client.sendRawTransaction(serialized, true);
		newChange();
		return new Sha256Hash(txId);
	}

	public boolean hasRKey() {
		return rkey != null;
	}

	public RpcClient getRpcClient() {
		return client;
	}

	synchronized public Rpc.Transaction[] listTransactions(int count, int from) {
		return client.listTransactions(label, count, from);
	}

	public static BigInteger toMicroCoins(double val) {
		return BigDecimal.valueOf(val).movePointRight(6).toBigIntegerExact();
	}
}
