package bank.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import bank.Account;
import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;
import bank.util.Command;
import bank.util.CommandName;
import bank.util.Result;

public class BankServer {
	private bank.local.Driver driver;
	private bank.Bank bank;
	private ServerSocket server;

	public static void main(String[] args) {
		int port = 6789;
		try {
			System.out.println("Starting bank server on port " + port);
			ExecutorService pool = Executors.newCachedThreadPool();
			BankServer bs = new BankServer();
			bs.server = new ServerSocket(port);
			bs.driver = new bank.local.Driver();
			bs.driver.connect(args);
			bs.bank = bs.driver.getBank();
			while (true) {
				Socket s = bs.server.accept();
				pool.execute(new ServerR(s, bs.bank));
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	public static class ServerR implements Runnable {

		Socket socket;
		InputStream sin;
		OutputStream sout;
		bank.Bank bank;

		public ServerR(Socket s, bank.Bank bank) {
			this.socket = s;
			this.bank = bank;
		}

		@Override
		public void run() {
			Command c;
			ObjectMapper mapper = new ObjectMapper();
			mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
			while (!socket.isClosed()) {
				try {
					sin = socket.getInputStream();
					JsonParser parser = mapper.getFactory().createParser(sin);
					c = mapper.readValue(parser, Command.class);
					// Framewok Jackson hat probleme, ein objekt zu mappen, wenn
					// als
					// ziel"klasse" ein interface genommen wird
					// Darum habe ich aus auf die hässliche variante ohne
					// Command
					// pattern gelöst.
					switch (c.name) {
					case deposit: {
						Result r;
						String number = c.arguments[0];
						double amount = Double.parseDouble(c.arguments[1]);
						Account acc = bank.getAccount(number);
						try {
							acc.deposit(amount);
							r = new Result(CommandName.deposit, null, null);
						} catch (InactiveException e) {
							r = new Result(CommandName.deposit, "Inactive", null);
						} catch (IllegalArgumentException e) {
							r = new Result(CommandName.deposit, "IllegalArgument", null);
						} catch (NullPointerException e) {
							r = new Result(CommandName.deposit, "IO", null);
						}
						sout = socket.getOutputStream();
						mapper.writeValue(sout, r);
						break;
					}
					case withdraw: {
						Result r;
						String number = c.arguments[0];
						double amount = Double.parseDouble(c.arguments[1]);
						Account acc = bank.getAccount(number);
						try {
							acc.withdraw(amount);
							r = new Result(CommandName.withdraw, null, null);
						} catch (InactiveException e) {
							r = new Result(CommandName.withdraw, "Inactive", null);
						} catch (IllegalArgumentException e) {
							r = new Result(CommandName.withdraw, "IllegalArgument", null);
						} catch (OverdrawException e) {
							r = new Result(CommandName.withdraw, "Overdraw", null);
						} catch (NullPointerException e) {
							r = new Result(CommandName.withdraw, "IO", null);
						}
						sout = socket.getOutputStream();
						mapper.writeValue(sout, r);
						break;
					}
					case transfer: {
						Result r;
						Account from = bank.getAccount(c.arguments[0]);
						Account to = bank.getAccount(c.arguments[1]);
						double amount = Double.parseDouble(c.arguments[2]);
						try {
							bank.transfer(from, to, amount);
							r = new Result(CommandName.transfer, null, null);
						} catch (InactiveException e) {
							r = new Result(CommandName.transfer, "Inactive", null);
						} catch (IllegalArgumentException e) {
							r = new Result(CommandName.transfer, "IllegalArgument", null);
						} catch (OverdrawException e) {
							r = new Result(CommandName.transfer, "Overdraw", null);
						} catch (NullPointerException e) {
							r = new Result(CommandName.transfer, "IO", null);
						}
						sout = socket.getOutputStream();
						mapper.writeValue(sout, r);
						break;
					}
					case createAccount: {
						String owner = c.arguments[0];
						Result r;
						try {
							String number = bank.createAccount(owner);
							r = new Result(CommandName.createAccount, null, number);
						} catch (NullPointerException e) {
							r = new Result(CommandName.createAccount, "IO", null);
						}
						sout = socket.getOutputStream();
						mapper.writeValue(sout, r);
						break;
					}
					case closeAccount: {
						String number = c.arguments[0];
						Result r;
						try {
							Boolean result = bank.closeAccount(number);
							r = new Result(CommandName.closeAccount, null, result);
						} catch (NullPointerException e) {
							r = new Result(CommandName.closeAccount,"IO", null);
						}
						sout = socket.getOutputStream();
						mapper.writeValue(sout, r);
						break;
					}
					case getAccountNumbers: {
						Result r;
						Set<String> result = bank.getAccountNumbers();
						r = new Result(CommandName.getAccountNumbers, null, result);
						sout = socket.getOutputStream();
						mapper.writeValue(sout, r);
						break;
					}
					case getAccount: {
						String number = c.arguments[0];
						sout = socket.getOutputStream();
						Account acc = bank.getAccount(number);
						String[] res = new String[2];
						if (acc != null) {
							res[0] = number;
							res[1] = bank.getAccount(number).getOwner();
						}else res = null;
						mapper.writeValue(sout, new Result(CommandName.getAccount, null, res));
						break;
					}
					case getBalance: {
						String number = c.arguments[0];
						sout = socket.getOutputStream();
						mapper.writeValue(sout,
								new Result(CommandName.getBalance, null, bank.getAccount(number).getBalance()));
						break;
					}
					case isActive: {
						String number = c.arguments[0];
						sout = socket.getOutputStream();
						mapper.writeValue(sout,
								new Result(CommandName.isActive, null, bank.getAccount(number).isActive()));
						break;
					}
					default:
						System.out.println("fail");
						break;
					}
				} catch (IOException e) {
					e.printStackTrace(System.err);
					try {
						socket.close();
						break;
					} catch (IOException e1) {
						break;
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}

}