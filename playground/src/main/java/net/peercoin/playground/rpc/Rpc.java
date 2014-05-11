package net.peercoin.playground.rpc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Rpc {
	public static class Unspent {
		public String txid;
		public long vout;
		public String address;
		public String account;
		public String scriptPubKey;
		public double amount;
		public long confirmations;

		@Override
		public String toString() {
			return "RpcUnspent [txid=" + txid + ", vout=" + vout + ", address="
					+ address + ", account=" + account + ", scriptPubKey="
					+ scriptPubKey + ", amount=" + amount + ", confirmations="
					+ confirmations + "]";
		}
	}

	public static class Transaction {
		public String account, address, category, blockhash, txid;
		public long time;
		public int confirmations, blockindex;
		public double amount, fee;

		@Override
		public String toString() {
			return "Transaction [account=" + account + ", address=" + address
					+ ", category=" + category + ", blockhash=" + blockhash
					+ ", txid=" + txid + ", time=" + time + ", confirmations="
					+ confirmations + ", blockindex=" + blockindex
					+ ", amount=" + amount + ", fee=" + fee + "]";
		}

	}

	public static class TransactionToCreate {
		public static class Input {
			public String txid;
			public long vout;

		}

		List<Input> inputs = new ArrayList<TransactionToCreate.Input>();
		Map<String, Double> outputs = new LinkedHashMap<String, Double>();

		public Object[] toArgs() {
			return new Object[] { inputs, outputs };
		}

		public void addInput(String txid, long vout) {
			Input in = new Input();
			in.txid = txid;
			in.vout = vout;
			inputs.add(in);
		}

		public void addOutput(String addr, double amount) {
			if (outputs.containsKey(addr))
				throw new RuntimeException("duplicated output address");
			outputs.put(addr, amount);
		}
	}

	public static class SigningResult {
		public String hex;
		public boolean complete;
	}

	public static class PubImportResult {
		public boolean added;
		public String address, account;

		@Override
		public String toString() {
			return "PubImportResult [added=" + added + ", address=" + address
					+ ", account=" + account + "]";
		}
	}

	public static class ValidateAddress {
		public boolean isvalid, ismine, isscript, iscompressed;
		public String address, pubkey, account;

		@Override
		public String toString() {
			return "ValidateAddress [isvalid=" + isvalid + ", ismine=" + ismine
					+ ", isscript=" + isscript + ", iscompressed="
					+ iscompressed + ", address=" + address + ", pubkey="
					+ pubkey + ", account=" + account + "]";
		}
	}
}