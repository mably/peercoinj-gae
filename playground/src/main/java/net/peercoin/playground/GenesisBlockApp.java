package net.peercoin.playground;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.params.PPCNetParams;
import com.google.bitcoin.params.PPCTestParams;

public class GenesisBlockApp {
	/*
		// Genesis Block:
	    // CBlock(hash=000000000019d6, ver=1, hashPrevBlock=00000000000000, hashMerkleRoot=4a5e1e, nTime=1231006505, nBits=1d00ffff, nNonce=2083236893, vtx=1)
	    //   CTransaction(hash=4a5e1e, ver=1, vin.size=1, vout.size=1, nLockTime=0)
	    //     CTxIn(COutPoint(000000, -1), coinbase 04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73)
	    //     CTxOut(nValue=50.00000000, scriptPubKey=0x5F1DF16B2B704C8A578D0B)
	    //   vMerkleTree: 4a5e1e

		const char* pszTimestamp = "Matonis 07-AUG-2012 Parallel Currencies And The Roadmap To Monetary Freedom";
	    CTransaction txNew;
	    txNew.nTime = 1345083810;
	    txNew.vin.resize(1);
	    txNew.vout.resize(1);
	    txNew.vin[0].scriptSig = CScript() << 486604799 << CBigNum(9999) << vector<unsigned char>((const unsigned char*)pszTimestamp, (const unsigned char*)pszTimestamp + strlen(pszTimestamp));
	    txNew.vout[0].SetEmpty();
	    CBlock block;
	    block.vtx.push_back(txNew);
	    block.hashPrevBlock = 0;
	    block.hashMerkleRoot = block.BuildMerkleTree();
	    block.nVersion = 1;
	    block.nTime    = 1345084287;
	    block.nBits    = bnProofOfWorkLimit.GetCompact();
	    block.nNonce   = 2179302059u;

	    if (fTestNet)
	    {
	        block.nTime    = 1345090000;
	        block.nNonce   = 122894938;
	    }

	 */
	public static void main(String[] args) {
		boolean testnet = false;
		NetworkParameters params = testnet ? PPCTestParams.get() : PPCNetParams
				.get();
		Transaction tx = new Transaction(params);
		tx.setTime(1345083810l);
		// tx.addInput(new TransactionInput(params, parentTransaction,
		// scriptBytes))

	}
}
