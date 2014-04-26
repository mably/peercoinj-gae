package net.peercoin.playground.rpc;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.PPCNetParams;
import com.google.bitcoin.params.PPCTestParams;

public class NetworkDetails {
	public final String name;
	public final String shortName;
	public final int rpcPort;
	public final String clientFolder;
	public final String clientConfig;
	public final long base;
	public final long blockTarget;
	public final NetworkParameters params;
	public final BigInteger minTxFee;
	public final BigInteger minTxOut;

	public NetworkDetails(String name, String shortName, int rpcPort,
			String clientFolder, String clientConfig, long base,
			NetworkParameters params, BigInteger minTxFee, BigInteger minTxOut,
			long blockTarget) {
		super();
		this.name = name;
		this.shortName = shortName;
		this.rpcPort = rpcPort;
		this.clientFolder = clientFolder;
		this.clientConfig = clientConfig;
		this.base = base;
		this.params = params;
		this.minTxFee = minTxFee;
		this.minTxOut = minTxOut;
		this.blockTarget = blockTarget;
	}

	static Map<String, NetworkDetails> details = new HashMap<String, NetworkDetails>();
	static {
		details.put(
				"PPC",
				new NetworkDetails("Peercoin", "PPC", 9902, "ppcoin",
						"ppcoin.conf", 1000000l, PPCNetParams.get(), BigInteger
								.valueOf(10000l), BigInteger.valueOf(10000l),
						60 * 10));
		details.put("PPC-testnet",
				new NetworkDetails("Peercoin-testnet", "PPC", 9904, "ppcoin",
						"ppcoin.conf", 1000000l, PPCTestParams.get(),
						BigInteger.valueOf(10000l), BigInteger.valueOf(10000l),
						60 * 10));
		details.put("BTC",
				new NetworkDetails("Bitcoin", "BTC", 9012, "bitcoin",
						"bitcoin.conf", 100000000l, MainNetParams.get(),
						BigInteger.valueOf(10000l), BigInteger.valueOf(10000l),
						60 * 10));
	}

	public static NetworkDetails get(String n) {
		return details.get(n);
	}
}