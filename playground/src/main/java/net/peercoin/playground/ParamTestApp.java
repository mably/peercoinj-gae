package net.peercoin.playground;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.PPCNetParams;
import com.google.bitcoin.params.PPCTestParams;

public class ParamTestApp {
	public static void main(String[] args) throws Exception {
		String[][] validAddresses = {
				{ "PDK4gHNWQHhhRHjEC72aDueehCgE1QEHqc",
						"pUdZ5NAutHdA7hm9jR14Q86FjBQ4BvM1cF" },
				{ "mu43BA1tTua9xS9KkAAwViA6QJHUMfdHDy",
						"2NGLCrwifyYENiwmzfZKEi2bQT2kaP5GFLH" } };
		NetworkParameters[] params = { PPCNetParams.get(), PPCTestParams.get() };
		ECKey key = new ECKey();
		for (int i = 0; i < params.length; i++) {
			String addrString = key.toAddress(params[i]).toString();
			Address addr = new Address(params[i], addrString);
			System.out.println(addr.toString().equals(addrString));
			String dumpedPrivKey = key.getPrivateKeyEncoded(params[i])
					.toString();
			// System.out.println(dumpedPrivKey);
			ECKey key2 = new DumpedPrivateKey(params[i], dumpedPrivKey)
					.getKey();
			System.out.println(key2.equals(key));
			// check valid
			for (String addrStr : validAddresses[i])
				new Address(params[i], addrStr);

		}
	}
}
