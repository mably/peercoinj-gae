package net.peercoin.playground;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

import net.peercoin.playground.rpc.NetworkDetails;
import net.peercoin.playground.rpc.Rpc;
import net.peercoin.playground.rpc.RpcClient;

import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.script.Script;

public class RpcTestsApp {

	public static void main(String[] args) throws Exception {
		NetworkDetails network = NetworkDetails.get("PPC-testnet");
		RpcClient client = new RpcClient(network);
		for (Rpc.Unspent unspent : client.listUnspent(true)) {
			System.out.println(unspent);
			Address addr = new Script(Hex.decode(unspent.scriptPubKey))
					.getToAddress(network.params);
			System.out.println(addr);
			byte[] txbytes = client.getRawTransactionBytes(unspent.txid);
			Transaction tx = new Transaction(network.params, txbytes);
			System.out.println(tx);
			System.out.println(Hex.toHexString(txbytes));
			txbytes = tx.bitcoinSerialize();
			System.out.println(Hex.toHexString(txbytes));
			tx = new Transaction(network.params, txbytes);
		}
		long t = System.currentTimeMillis();
		client.listUnspent(false);
		System.out.println(System.currentTimeMillis() - t);
		for (Map.Entry<String, Double> account : client.listAccounts(false, 0)
				.entrySet()) {
			System.out.println(account);
		}

		System.out.println(client.getAddressesByAccount(true, ""));

		ECKey key;

		if (true)
			key = new ECKey();
		else
			key = new ECKey(
					new BigInteger(
							1,
							Hex.decode("0be0b59d963a86d52668ff8393a1c88254197ecf615ca8ec60eb9acf15e9e3e0")),
					null, true);

		System.out.println(key.getPrivateKeyEncoded(network.params));
		System.out.println(key.toAddress(network.params));
		try {
			String label = "test " + new Random().nextInt(100);
			//label = "";
			String newAddress = client.pubImportKey(
					Hex.toHexString(key.getPubKey()), label, false);
			System.out.println("new Address '" + label + "' " + newAddress);
		} catch (Exception e) {

		}
	}

	/*
	cQZE7hp2bq6FQ5JHLHn9zrx8kFVXsocPV9o4pxwePxide34BtgVX
	n3zSQhmbvWN8gfQHrjXKugwtGrPHtsmFHC
	new Address 'test 21' n3zSQhmbvWN8gfQHrjXKugwtGrPHtsmFHC
	 */
}
