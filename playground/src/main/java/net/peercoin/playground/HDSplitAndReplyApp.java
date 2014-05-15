package net.peercoin.playground;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import net.peercoin.playground.hd.XKey;
import net.peercoin.playground.hd.XKeyFile;
import net.peercoin.playground.hd.XClient;
import net.peercoin.playground.rpc.Rpc;
import net.peercoin.playground.rpc.RpcClient;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.params.PPCNetParams;
import com.google.bitcoin.params.PPCTestParams;

public class HDSplitAndReplyApp {

	public static void main(String[] args) throws Exception {
		boolean prodNet = false;
		NetworkParameters params = prodNet ? PPCNetParams.get() : PPCTestParams.get();

		XClient xclient = createClient(prodNet);
		xclient.syncClientWithKey();

		// create new address (main wallet)
		String newAddr = xclient.newKey().toAddress(params).toString();
		System.out.println("send coins to " + newAddr);
		// wait for coins
		Rpc.Unspent[] unspent;
		while ((unspent = xclient.listUnpsent(newAddr)).length == 0) {
			Thread.sleep(1000);
		}
		BigInteger receivedAmount = XClient.toMicroCoins(unspent[0].amount);
		// send half back to the sender and half to out cold storage
		BigInteger sendToEach = receivedAmount.subtract(Utils.CENT).subtract(Utils.CENT)
				.divide(BigInteger.valueOf(2));
		// get and parse raw transaction
		Transaction tx = new Transaction(params, xclient.getRpcClient().getRawTransactionBytes(
				unspent[0].txid));
		// extract sender's address
		String fromAddress = tx.getInput(0).getFromAddress().toString();
		// send to the sender
		xclient.sendToAddress(new Address(params, fromAddress), sendToEach);
		// send to new cold storage key
		xclient.sendToAddress(xclient.newRKey().toAddress(params), sendToEach);
	}

	static XClient createClient(boolean prodNet) throws Exception {
		RpcClient client = Bip0032PublicWalletPingPongApp.createRpcClient(prodNet);
		client.setMainWallet(false);
		File keyDir = new File("testData");
		keyDir.mkdirs();
		File keyFile = new File(keyDir, "xclient.key0.csv"), rkeyFile = new File(keyDir,
				"xclient.rkey0.csv");
		XKey key = new XKeyFile(
				keyFile,
				keyFile.exists() ? null
						: DeterministicKey
								.deserializeB58(
										null,
										"xprv9s21ZrQH143K2yUwSoENrPfNQTsezKJRpkmcCHPScNnvzvWorjW5kNA4wbhcqw2ZRUku6QNYFavHWCBE4GEWYDC6GSYLM2Aeo1HzVLevoZs"),
				null);
		XKey rkey = new XKeyFile(
				rkeyFile,
				rkeyFile.exists() ? null
						: DeterministicKey
								.deserializeB58(
										null,
										"xpub661MyMwAqRbcFg1LWRwYsbB2xX8Vq3BH2cT1QDyBWVpnvdbT3HDCCz8GMFrBBmqt4EF7X6dBh14DBA3fLFLDUsXN2ke1UHJzhbcp3A9eynJ"),
				null);

		return new XClient(key, rkey, client, prodNet ? PPCNetParams.get() : PPCTestParams.get());
	}
}
