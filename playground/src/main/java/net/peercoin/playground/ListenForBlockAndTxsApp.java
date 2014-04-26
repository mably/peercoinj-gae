package net.peercoin.playground;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.InventoryItem;
import com.google.bitcoin.core.InventoryMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.params.PPCNetParams;
import com.google.bitcoin.params.PPCTestParams;
import com.google.bitcoin.utils.Threading;

public class ListenForBlockAndTxsApp {

	public static void main(String[] args) throws Exception {

		final boolean testnet = true;
		final NetworkParameters params = testnet ? PPCTestParams.get()
				: PPCNetParams.get();

		final PeerGroup group;
		group = new PeerGroup(params);
		group.setPingIntervalMsec(1000l * 10);

		group.addEventListener(new AbstractPeerEventListener() {
			@Override
			public Message onPreMessageReceived(final Peer peer, final Message m) {
				if (m instanceof InventoryMessage) {
					for (final InventoryItem item : ((InventoryMessage) m)
							.getItems()) {
						if (item.type == InventoryItem.Type.Block)
							new Thread() {
								public void run() {
									try {
										Block block = peer.getBlock(item.hash)
												.get();
										System.out.println("*** NEW BLOCK ***");
										System.out.println(block);
									} catch (Exception e) {
										e.printStackTrace();
									}
								};
							}.start();
					}

				} else if (m instanceof Transaction) {
					System.out.println("*** NEW TX ***");
					System.out.println(m);
				}

				return m;
			}
		}, Threading.SAME_THREAD);

		group.addAddress(new PeerAddress(new InetSocketAddress("localhost",
				params.getPort())));
		// g.addPeerDiscovery(new DnsDiscovery(params));

		group.startAndWait();

		// wait for connection
		while (group.getConnectedPeers().size() < 1)
			Thread.sleep(500);

		// get one block
		Block block;

		block = group
				.getConnectedPeers()
				.get(0)
				.getBlock(
						new Sha256Hash(
								testnet ? "00000001f757bb737f6596503e17cd17b0658ce630cc727c0cca81aec47c9f06"
										: "00000001f757bb737f6596503e17cd17b0658ce630cc727c0cca81aec47c9f06"))
				.get();
		System.out.println("genesis " + block);
		String genesisHex = Hex.toHexString(block.bitcoinSerialize());
		System.out.println("genesisHex "+genesisHex);
		block = new Block(params, Hex.decode(genesisHex));
		System.out.println("parsed genesis " + block);

		block = group
				.getConnectedPeers()
				.get(0)
				.getBlock(
						new Sha256Hash(
								testnet ? "0000000bbf7e87ac4089f6ec268ed8b3524d00e735d7ee3c5fb7b410ab240a42"
										: "3804075256ee3854d8760f6a6db6ec735adcd57cd05d5d0a1b861ab5a108b5aa"))
				.get();
		System.out.println("block 108xxx " + block);

		System.in.read();

		group.stopAndWait();
	}
}
