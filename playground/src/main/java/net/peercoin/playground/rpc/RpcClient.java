package net.peercoin.playground.rpc;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.spongycastle.util.encoders.Hex;

import com.google.common.reflect.TypeToken;
import com.googlecode.jsonrpc4j.Base64;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

public class RpcClient {
	JsonRpcHttpClient client;
	NetworkDetails network;

	public RpcClient() {
		// TODO Auto-generated constructor stub
	}

	public RpcClient(NetworkDetails network) throws Exception {
		super();
		this.network = network;
		initRpc();
	}

	void initRpc() throws Exception {
		File cfgFile = new File(System.getProperty("user.home"), "."
				+ network.clientFolder);
		cfgFile = new File(cfgFile, network.clientConfig);
		Properties cfg = new Properties();
		cfg.load(new FileReader(cfgFile));

		URL url = new URL("http://localhost:" + network.rpcPort + "/");
		client = new JsonRpcHttpClient(url);
		Map<String, String> headers = new HashMap<String, String>(
				client.getHeaders());
		headers.put(
				"Authorization",
				"Basic "
						+ Base64.encodeBytes((cfg.getProperty("rpcuser") + ':' + cfg
								.getProperty("rpcpassword")).getBytes("UTF-8")));
		client.setHeaders(headers);
	}

	public Rpc.Unspent[] listUnspent(String... addrs) {
		return listUnspent(true, addrs);
	}

	public Rpc.Unspent[] listUnspent(boolean mainWallet, String... addrs) {
		return listUnspent(mainWallet, 0, 9999999, addrs);
	}

	public Rpc.Unspent[] listUnspent(boolean mainWallet, int minconf,
			int maxconf, String... addrs) {
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
	public Map<String, Double> listAccounts(boolean mainWallet, int minconf) {
		try {
			return (Map) client.invoke((mainWallet ? "" : "pub")
					+ "listaccounts", new Object[] { minconf },
					new TypeToken<LinkedHashMap<String, Double>>() {
					}.getType());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public List<String> getAddressesByAccount(boolean mainWallet, String account) {
		try {
			return (List) client.invoke((mainWallet ? "" : "pub")
					+ "getaddressesbyaccount", new Object[] { account },
					new TypeToken<List<String>>() {
					}.getType());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String pubImportKey(String hexPubKey, String label, boolean rescan) {
		try {
			return client.invoke("pubimportkey", new Object[] { hexPubKey,
					label, rescan }, String.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}