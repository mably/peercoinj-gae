package net.peercoin.playground;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Properties;

import net.peercoin.playground.rpc.Rpc;
import net.peercoin.playground.rpc.RpcClient;
import net.peercoin.playground.spongy.Hex;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.params.PPCNetParams;
import com.google.bitcoin.params.PPCTestParams;
import com.google.bitcoin.script.ScriptBuilder;

public class Bip0032PublicWalletPingPongApp {
	public static RpcClient createRpcClient(boolean prodNet) throws Exception {
		File cfgFile = new File(System.getProperty("user.home"), ".ppcoin");
		cfgFile = new File(cfgFile, "ppcoin.conf");
		Properties cfg = new Properties();
		cfg.load(new FileReader(cfgFile));

		return new RpcClient("http://localhost:" + (prodNet ? 9902 : 9904)
				+ "/", cfg.getProperty("rpcuser"),
				cfg.getProperty("rpcpassword"));
	}

	static void assertTrue(boolean condition, String message) {
		if (!condition)
			throw new RuntimeException("assert"
					+ (message != null ? ": " + message : ""));
	}

	static void assertTrue(boolean condition) {
		assertTrue(condition, null);
	}

	public static void main(String[] args) throws Exception {

		String decryptedWallet = "xprv9s21ZrQH143K2yUwSoENrPfNQTsezKJRpkmcCHPScNnvzvWorjW5kNA4wbhcqw2ZRUku6QNYFavHWCBE4GEWYDC6GSYLM2Aeo1HzVLevoZs";

		boolean prodNet = false;
		double txFee = 0.01;
		BigInteger txFeeMicro = BigDecimal.valueOf(txFee).movePointRight(6)
				.toBigIntegerExact();
		double toSend = 1;
		boolean send = true;

		NetworkParameters params = prodNet ? PPCNetParams.get() : PPCTestParams
				.get();
		RpcClient client = createRpcClient(prodNet);

		double mainWalletBalance = client.getBalance("*", 1);
		assertTrue(mainWalletBalance > toSend + txFee);
		// switch wallet to public mode
		client.setMainWallet(false);

		DeterministicKey key;
		key = DeterministicKey.deserializeB58(null, decryptedWallet);

		String address = key.toAddress(params).toString();
		String account = "hd_" + address;

		Rpc.PubImportResult importResult = client.pubImportKey(
				Hex.toHexString(key.getPubKey()), account, false);
		System.out.println(importResult);
		assertTrue(address.equals(importResult.address));
		// TODO if (!importResult.account.equals(account))
		// ;

		String lastTxId = "none";
		for (Rpc.Transaction tx : client.listTransactions(account, 1, 0)) {
			lastTxId = tx.txid;
			break;
		}

		String txId = null;
		if (send) {
			txId = client.sendToAddress(address, toSend, "to public wallet",
					null);
			System.out.println(txId);
			// let tx propagate through client wallets
			Thread.sleep(100);
		}

		Rpc.Transaction[] txs = client.listTransactions(account, 1, 0);
		assertTrue(1 == txs.length);
		if (!send)
			txId = txs[0].txid;
		assertTrue(txId.equals(txs[0].txid));

		byte[] txBytes = client.getRawTransactionBytes(txId);
		Transaction tx = new Transaction(params, txBytes);
		System.out.println(tx);

		// find address that sent to us
		Address fromAddress = null;
		for (TransactionInput input : tx.getInputs())
			try {
				fromAddress = input.getFromAddress();
				break;
			} catch (Exception e) {
			}

		assertTrue(fromAddress != null);

		// find our unspent output
		int outputNum = -1;
		for (int i = 0; i < tx.getOutputs().size(); i++)
			try {
				Address addr = tx.getOutput(i).getScriptPubKey()
						.getToAddress(params);
				if (address.equals(addr.toString())) {
					outputNum = i;
					break;
				}
			} catch (Exception e) {
			}
		assertTrue(outputNum >= 0);
		assertTrue(tx.getOutput(outputNum).getValue().doubleValue()
				/ Utils.COIN.doubleValue() > txFee);

		// create PONG tx
		Transaction pongTx = new Transaction(params);
		pongTx.addInput(tx.getOutput(outputNum));
		pongTx.addOutput(BigInteger.ZERO, fromAddress);
		pongTx.setTime(System.currentTimeMillis() / 1000);

		BigInteger pongSend, pongFee, pongRequiredFee;
		pongRequiredFee = txFeeMicro;
		byte[] serialized;
		do {
			pongFee = pongRequiredFee;
			pongSend = tx.getOutput(outputNum).getValue().subtract(pongFee);
			assertTrue(pongSend.compareTo(txFeeMicro) > 0);
			pongTx.getOutput(0).setValue(pongSend);
			// create signature
			TransactionSignature sig = pongTx
					.calculateSignature(0, key, tx.getOutput(outputNum)
							.getScriptPubKey(), SigHash.ALL, false);
			// set signature
			pongTx.getInput(0).setScriptSig(
					ScriptBuilder.createInputScript(sig, key));
			serialized = pongTx.bitcoinSerialize();
			pongRequiredFee = txFeeMicro.multiply(BigInteger
					.valueOf(serialized.length / 1000 + 1));
		} while (pongFee.compareTo(pongRequiredFee) < 0);

		System.out.println("fee " + pongFee);
		System.out.println(pongTx);

		// send
		String repTxId = client.sendRawTransaction(serialized, true);
		// voilà!
		System.out.println(repTxId);
	}
}
