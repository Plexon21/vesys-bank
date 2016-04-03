package bank.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import bank.Account;
import bank.Bank;
import bank.BankDriver;
import bank.InactiveException;
import bank.OverdrawException;
import bank.sockets.SocketDriver.SocketAccount;
import bank.util.Command;
import bank.util.CommandName;
import bank.util.Result;

public class HttpDriver implements BankDriver {
	private HttpBank bank = null;

	@Override
	public void connect(String[] args) throws IOException {
		bank = new HttpBank(args[0], Integer.parseInt(args[1]));
	}

	@Override
	public void disconnect() throws IOException {
		bank = null;
	}

	@Override
	public Bank getBank() {
		return bank;
	}

	private class HttpBank implements bank.Bank {
		String address;
		int port;
		ObjectMapper mapper;

		private HttpBank(String address, int port) {
			this.address = address;
			this.port = port;
			mapper = new ObjectMapper();
			mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
		}

		private Result request(Command c) throws IOException {
			URL url = new URL("http", address, port, "/httpbank/bank");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
			con.connect();

			mapper.writeValue(con.getOutputStream(), c);
			JsonParser parser = mapper.getFactory().createParser(con.getInputStream());
			return mapper.readValue(parser, Result.class);
		}

		@Override
		public String createAccount(String owner) throws IOException {
			try {
				Result r = request(new Command(CommandName.createAccount, new String[] { owner }));
				return (String) r.resultValue;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public boolean closeAccount(String number) throws IOException {
			try {
				Result r = request(new Command(CommandName.closeAccount, new String[] { number }));
				return (boolean) r.resultValue;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return false;
			}
		}

		@Override
		public Set<String> getAccountNumbers() throws IOException {
			try {
				Result r = request(new Command(CommandName.getAccountNumbers, null));
				return new HashSet<String>((Collection<? extends String>) r.resultValue);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public Account getAccount(String number) throws IOException {
			try {
				Result r = request(new Command(CommandName.getAccount, new String[] { number }));
				ArrayList<String> res = (ArrayList<String>) r.resultValue;
				if (res != null)
					return new HttpAccount(res.get(1), res.get(0));
				else
					return null;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public void transfer(Account from, Account to, double amount)
				throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
			Result r = new Result();
			try {
				r = request(new Command(CommandName.transfer,
						new String[] { from.getNumber(), to.getNumber(), String.valueOf(amount) }));
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			if (r.exception != null) {
				switch (r.exception) {
				case "Inactive":
					throw new InactiveException();
				case "IllegalArgument":
					throw new IllegalArgumentException();
				case "Overdraw":
					throw new OverdrawException();
				case "IO":
					throw new IOException();
				}
			}

		}

		private class HttpAccount implements bank.Account {
			private String number;
			private String owner;
			private double balance;

			public HttpAccount() {

			}

			public HttpAccount(String owner, String number) {
				this.owner = owner;
				this.number = number;
			}

			@Override
			public String getNumber() throws IOException {
				return number;
			}

			@Override
			public String getOwner() throws IOException {
				return owner;
			}

			@Override
			public boolean isActive() throws IOException {
				Result r = request(new Command(CommandName.isActive, new String[] { getNumber() }));
				return (boolean) r.resultValue;
			}

			@Override
			public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
				if (isActive()) {
					Result r = request(
							new Command(CommandName.deposit, new String[] { getNumber(), String.valueOf(amount) }));
					if (r.exception != null) {
						switch (r.exception) {
						case "Inactive":
							throw new InactiveException();
						case "IllegalArgument":
							throw new IllegalArgumentException();
						case "IO":
							throw new IOException();
						}
					}
				} else
					throw new InactiveException();
			}

			@Override
			public void withdraw(double amount)
					throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
				if (isActive()) {
					Result r = request(
							new Command(CommandName.withdraw, new String[] { getNumber(), String.valueOf(amount) }));
					if (r.exception != null) {
						switch (r.exception) {
						case "Inactive":
							throw new InactiveException();
						case "IllegalArgument":
							throw new IllegalArgumentException();
						case "Overdraw":
							throw new OverdrawException();
						case "IO":
							throw new IOException();
						}
					}
				} else
					throw new InactiveException();
			}

			@Override
			public double getBalance() throws IOException {
				Result r = request(new Command(CommandName.getBalance, new String[] { getNumber() }));
				return (double) r.resultValue;
			}
		}
	}
}
