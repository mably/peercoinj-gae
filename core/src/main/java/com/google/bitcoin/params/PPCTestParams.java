package com.google.bitcoin.params;

import static com.google.common.base.Preconditions.checkState;

import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;

public class PPCTestParams extends NetworkParameters {
	public PPCTestParams() {

		super();
		interval = INTERVAL;
		targetTimespan = TARGET_TIMESPAN;
		proofOfWorkLimit = Utils.decodeCompactBits(0x1d00ffffL);
		addressHeader = 111;
		p2shHeader = 196;
		dumpedPrivateKeyHeader = addressHeader + 128;
		acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
		port = 9903;
		packetMagic = 0xcbf2c0ef;
		genesisBlock.setDifficultyTarget(0x1d00ffffL);// todo
														// bnProofOfWorkLimit.GetCompact();
		genesisBlock.setTime(1345084287L);
		genesisBlock.setNonce(2179302059l);
		id = "net.peercoint.testnet";
		subsidyDecreaseBlockCount = 210000;
		spendableCoinbaseDepth = 100;
		alertSigningKey = Hex
				.decode("04a0a849dd49b113d3179a332dd77715c43be4d0076e2f19e66de23dd707e56630f792f298dfd209bf042bb3561f4af6983f3d81e439737ab0bf7f898fecd21aab");// SunnyKing
		String genesisHash = genesisBlock.getHashAsString();
		if (false)
			checkState(
					genesisHash
							.equals("00000001f757bb737f6596503e17cd17b0658ce630cc727c0cca81aec47c9f06"),
					genesisHash);

		// This contains (at a minimum) the blocks which are not BIP30
		// compliant. BIP30 changed how duplicate
		// transactions are handled. Duplicated transactions could occur in the
		// case where a coinbase had the same
		// extraNonce and the same outputs but appeared at different heights,
		// and greatly complicated re-org handling.
		// Having these here simplifies block connection logic considerably.
		/*
		checkpoints.put(91722, new Sha256Hash("00000000000271a2dc26e7667f8419f2e15416dc6955e5a6c6cdf3f2574dd08e"));
		checkpoints.put(91812, new Sha256Hash("00000000000af0aed4792b1acee3d966af36cf5def14935db8de83d6f9306f2f"));
		checkpoints.put(91842, new Sha256Hash("00000000000a4d0a398161ffc163c503763b1f4360639393e0e4c8e300e0caec"));
		checkpoints.put(91880, new Sha256Hash("00000000000743f190a18c5577a3c2d2a1f610ae9601ac046a38084ccb7cd721"));
		checkpoints.put(200000, new Sha256Hash("000000000000034a7dedef4a161fa058a2d67a173a90155f3a2fe6fc132e0ebf"));
		*/
		dnsSeeds = new String[] { "tnseed.ppcoin.net" };
	}

	private static PPCTestParams instance;

	public static synchronized PPCTestParams get() {
		if (instance == null) {
			instance = new PPCTestParams();
		}
		return instance;
	}

	@Override
	public String getPaymentProtocolId() {
		// TODO Auto-generated method stub
		return null;
	}
}
