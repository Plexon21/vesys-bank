package bank.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;
import bank.util.Command;
import bank.util.CommandName;
import bank.util.Result;

public class SocketDriver implements bank.BankDriver {
	private SocketBank bank = null;
	ObjectMapper mapper;
	InputStream sin;
	OutputStream sout;
	Socket s;

	@Override
	public void connect(String[] args) throws IOException {
		try {
			s = new Socket(args[0], Integer.parseInt(args[1]), null, 0);
			System.out.println(s.isConnected());
			mapper = new ObjectMapper();
			mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
			sin = s.getInputStream();
			sout = s.getOutputStream();
			System.out.println("Connected to Server on " + args[0] + " " + args[1]);
			bank = new SocketBank();
		} catch (IOException e) {
			System.err.println("Could not connect to Server on " + args[0] + " " + args[1]);
		}
	}

	@Override
	public void disconnect() throws IOException {
		s.close();

	}

	@Override
	public Bank getBank() {
		return bank;
	}

	private class SocketBank implements bank.Bank {

		@Override
		public Set<String> getAccountNumbers() {
			try {
				sout = s.getOutputStream();
				mapper.writeValue(sout, new Command(CommandName.getAccountNumbers, null));
				sin = s.getInputStream();
				JsonParser parser = mapper.getFactory().createParser(sin);
				Result r = mapper.readValue(parser, Result.class);
				return new HashSet<String>((Collection<? extends String>) r.resultValue);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public String createAccount(String owner) {
			try {
				sout = s.getOutputStream();
				mapper.writeValue(sout, new Command(CommandName.createAccount, new String[] { owner }));
				sin = s.getInputStream();
				JsonParser parser = mapper.getFactory().createParser(sin);
				Result r = mapper.readValue(parser, Result.class);
				return (String) r.resultValue;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}

		@Override
		public boolean closeAccount(String number) {
			try {
				sout = s.getOutputStream();
				mapper.writeValue(sout, new Command(CommandName.closeAccount, new String[] { number }));
				sin = s.getInputStream();
				JsonParser parser = mapper.getFactory().createParser(sin);
				Result r = mapper.readValue(parser, Result.class);
				return (boolean) r.resultValue;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return false;
			}
		}

		@Override
		public bank.Account getAccount(String number) throws IOException {
			try {
				sout = s.getOutputStream();
				mapper.writeValue(sout, new Command(CommandName.getAccount, new String[] { number }));
				sin = s.getInputStream();
				JsonParser parser = mapper.getFactory().createParser(sin);
				Result r = mapper.readValue(parser, Result.class);
				ArrayList<String> res = (ArrayList<String>) r.resultValue;
				if (res != null)
					return new SocketAccount(res.get(1), res.get(0));
				else
					return null;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}

		}

		@Override
		public void transfer(bank.Account from, bank.Account to, double amount)
				throws IOException, InactiveException, OverdrawException, IllegalArgumentException {
			sout = s.getOutputStream();
			mapper.writeValue(sout, new Command(CommandName.transfer,
					new String[] { from.getNumber(), to.getNumber(), String.valueOf(amount) }));
			sin = s.getInputStream();
			JsonParser parser = mapper.getFactory().createParser(sin);
			Result r = mapper.readValue(parser, Result.class);
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

	}

	public class SocketAccount implements bank.Account {
		private String number;
		private String owner;
		private double balance;

		public SocketAccount() {

		}

		public SocketAccount(String owner, String number) {
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
			sout = s.getOutputStream();
			mapper.writeValue(sout, new Command(CommandName.isActive, new String[] { getNumber() }));
			sin = s.getInputStream();
			JsonParser parser = mapper.getFactory().createParser(sin);
			Result r = mapper.readValue(parser, Result.class);
			return (boolean) r.resultValue;
		}

		@Override
		public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
			if (isActive()) {
				sout = s.getOutputStream();
				mapper.writeValue(sout,
						new Command(CommandName.deposit, new String[] { getNumber(), String.valueOf(amount) }));
				sin = s.getInputStream();
				JsonParser parser = mapper.getFactory().createParser(sin);
				Result r = mapper.readValue(parser, Result.class);
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
				sout = s.getOutputStream();
				mapper.writeValue(sout,
						new Command(CommandName.withdraw, new String[] { getNumber(), String.valueOf(amount) }));
				sin = s.getInputStream();
				JsonParser parser = mapper.getFactory().createParser(sin);
				Result r = mapper.readValue(parser, Result.class);
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
			sout = s.getOutputStream();
			mapper.writeValue(sout, new Command(CommandName.getBalance, new String[] { getNumber() }));
			sin = s.getInputStream();
			JsonParser parser = mapper.getFactory().createParser(sin);
			Result r = mapper.readValue(parser, Result.class);
			return (double) r.resultValue;
		}

	}

}
