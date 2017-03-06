package org.matthiaszimmermann.web3j.demo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

/**
 * Sample application to demonstrate the basic usage of the web3j library. 
 */
public class HelloWorld {

	static final BigInteger GAS_PRICE = BigInteger.valueOf(9_000L);
	static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_000_000L);

	static Web3j web3j = null;

	public static void main(String [] args) throws Exception  {
		String port = "8545";
		String ip = "172.17.0.2";
		if(args.length >= 1) { port = args[0]; }
		if(args.length >= 2) { ip = args[1]; }
		
		HelloWorld helloWorld = new HelloWorld(ip, port);
		helloWorld.runHelloWorld();
	}

	public HelloWorld(String ip, String port) {
		String url = String.format("http://%s:%s", ip, port);
		web3j = Web3j.build(new HttpService(url));
	}

	public void runHelloWorld() throws Exception{
		Web3ClientVersion client = web3j.web3ClientVersion().sendAsync().get();
		System.out.println("Connected to " + client.getWeb3ClientVersion());

		EthAccounts accounts = web3j.ethAccounts().sendAsync().get();

		String sendingAddress = accounts.getResult().get(0);
		String receivingAddress = accounts.getResult().get(1);
		BigInteger value = Convert.toWei("0.124", Convert.Unit.ETHER)
				.toBigInteger();
		
		System.out.println("Before TX:");
		printAccountBallance(sendingAddress, "sendingAddress");
		printAccountBallance(receivingAddress, "receivingAddress");

		String txHash = transfer(sendingAddress, receivingAddress, value);
		waitForReceipt(txHash);

		System.out.println("After TX:");
		printAccountBallance(sendingAddress, "sendingAddress");
		printAccountBallance(receivingAddress, "receivingAddress");
		}

	private void printAccountBallance(String address, String accountName) throws Exception {
		System.out.println(String.format("%s %s balance : %s", accountName, address, getBalance(address)));
	}

	BigDecimal getBalance(String address) throws Exception {
		EthGetBalance balance = web3j
				.ethGetBalance(address, DefaultBlockParameterName.LATEST)
				.sendAsync()
				.get();

		return Convert.fromWei(
				balance.getBalance().toString(), Convert.Unit.ETHER);
	}

	String transfer(String from, String to, BigInteger value) 
			throws Exception 
	{
		EthGetTransactionCount transactionCount = web3j
				.ethGetTransactionCount(from, DefaultBlockParameterName.LATEST)
				.sendAsync()
				.get();

		BigInteger nonce = transactionCount.getTransactionCount();
		Transaction transaction = Transaction.createEtherTransaction(
				from, nonce, GAS_PRICE, GAS_LIMIT, to, value);

		EthSendTransaction response = web3j
				.ethSendTransaction(transaction)
				.sendAsync()
				.get();
		
		if (response.getError() != null) 
		{
			throw new RuntimeException("Transaction error: " + response.getError().getMessage());
		}

		return response.getTransactionHash();
	}

	TransactionReceipt waitForReceipt(String transactionHash) 
			throws Exception 
	{

		Optional<TransactionReceipt> receipt = getReceipt(transactionHash);
		int attempts = 40;

		while(attempts-- > 0 && !receipt.isPresent()) {
			Thread.sleep(1000);
			receipt = getReceipt(transactionHash);
		}

		if (attempts <= 0) {
			throw new RuntimeException("No Tx receipt received");
		}

		return receipt.get();
	}

	Optional<TransactionReceipt> getReceipt(String transactionHash) 
			throws Exception 
	{
		EthGetTransactionReceipt receipt = web3j
				.ethGetTransactionReceipt(transactionHash)
				.sendAsync()
				.get();

		return receipt.getTransactionReceipt();
	}
}

