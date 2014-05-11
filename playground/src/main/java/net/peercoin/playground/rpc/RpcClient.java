package net.peercoin.playground.rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.peercoin.playground.rpc.Rpc.ValidateAddress;
import ppc.spongy.Hex;

import com.google.common.reflect.TypeToken;
import com.googlecode.jsonrpc4j.Base64;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

public class RpcClient {
	static final Charset UTF8 = Charset.forName("UTF-8");

	JsonRpcHttpClient client;
	boolean mainWallet = true;

	public RpcClient(String url, String rpcUser, String rpcPassword)
			throws MalformedURLException {
		super();

		client = new JsonRpcHttpClient(new URL(url));
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(
				"Authorization",
				"Basic "
						+ Base64.encodeBytes((rpcUser + ':' + rpcPassword)
								.getBytes(UTF8)));
		client.setHeaders(headers);
	}

	public Rpc.Unspent[] listUnspent(String... addrs) {
		return listUnspent(1, 9999999, addrs);
	}

	public Rpc.Unspent[] listUnspent(int minconf, int maxconf, String... addrs) {
		try {
			return client.invoke((mainWallet ? "" : "pub") + "listunspent",
					new Object[] { minconf, maxconf, addrs },
					Rpc.Unspent[].class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String dumpPrivKey(String addr) {
		try {
			return client.invoke("dumpprivkey", new Object[] { addr },
					String.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] getRawTransactionBytes(String txid) {
		try {
			return Hex.decode(client.invoke("getrawtransaction",
					new Object[] { txid }, String.class));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public Map<String, Double> listAccounts(int minconf) {
		try {
			return (Map) client.invoke(m("listaccounts"),
					new Object[] { minconf },
					new TypeToken<LinkedHashMap<String, Double>>() {
					}.getType());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String[] getAddressesByAccount(String account) {
		try {
			return client.invoke(m("getaddressesbyaccount"),
					new Object[] { account }, String[].class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public Rpc.Transaction[] listTransactions(String account, int count,
			int from) {
		try {
			return client.invoke(m("listtransactions"), new Object[] { account,
					count, from }, Rpc.Transaction[].class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public Double getBalance(String account, int minconf) {
		try {
			return client.invoke(m("getbalance"), new Object[] { account,
					minconf }, Double.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String sendToAddress(String address, double amount, String comment,
			String commentTo) {
		Object[] args = new Object[comment != null ? commentTo != null ? 4 : 3
				: 2];
		args[0] = address;
		args[1] = amount;
		if (comment != null) {
			args[2] = comment;
			if (commentTo != null)
				args[3] = commentTo;
		}
		try {
			return client.invoke("sendtoaddress", args, String.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public Rpc.PubImportResult pubImportKey(String hexPubKey, String label,
			boolean rescan) {
		try {
			return client.invoke("pubimportkey", new Object[] { hexPubKey,
					label, rescan }, Rpc.PubImportResult.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String sendRawTransaction(byte[] tx) {
		try {
			return client.invoke("sendrawtransaction",
					new Object[] { Hex.toHexString(tx) }, String.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String sendRawTransaction(byte[] tx, boolean checkInputs) {
		try {
			return client.invoke("sendrawtransaction",
					new Object[] { Hex.toHexString(tx), checkInputs ? 1 : 0 },
					String.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public Rpc.ValidateAddress validateAddress(String address) {
		try {
			return client.invoke(m("sendrawtransaction"),
					new Object[] { address }, ValidateAddress.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	protected String m(String method) {
		return mainWallet ? method : "pub" + method;
	}

	public void setMainWallet(boolean mainWallet) {
		this.mainWallet = mainWallet;
	}

	public boolean isMainWallet() {
		return mainWallet;
	}
}