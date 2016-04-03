/*
 * Copyright (c) 2000-2016 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package bank.local;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import bank.InactiveException;
import bank.OverdrawException;

public class Driver implements bank.BankDriver {
	private Bank bank = null;

	@Override
	public void connect(String[] args) {
		bank = new Bank();
		System.out.println("connected...");
		
	}

	@Override
	public void disconnect() {
		bank = null;
		System.out.println("disconnected...");
	}

	@Override
	public Bank getBank() {
		return bank;
	}

	static class Bank implements bank.Bank {

		public int number = 0;
		private final Map<String, Account> accounts = new HashMap<>();

		@Override
		public Set<String> getAccountNumbers() {
			return Collections.unmodifiableSet(accounts.values().stream().filter(acc -> acc.isActive())
					.map(acc -> acc.number).collect(Collectors.toSet()));
		}

		@Override
		public String createAccount(String owner) {
			if (owner != null) {
				Account acc = new Account(owner, number);
				accounts.put(acc.getNumber(), acc);
				number++;
				return accounts.get(acc.getNumber()).getNumber();
			} else
				return null;
		}

		@Override
		public boolean closeAccount(String number) {
			Account acc = accounts.get(number);
			if (acc.active && acc.balance == 0) {
				acc.active = false;
				return true;
			}
			return false;
		}

		@Override
		public bank.Account getAccount(String number) {
			Account acc = accounts.get(number);
			return acc;

		}

		@Override
		public void transfer(bank.Account from, bank.Account to, double amount)
				throws IOException, InactiveException, OverdrawException {
			if (!from.getNumber().equals(to.getNumber())) {
				if (!from.isActive() || !to.isActive()) {
					throw new InactiveException();
				}
				if (amount <= 0) {
					throw new IllegalArgumentException("IllegalArgument");
				}
				if (amount > from.getBalance()) {
					throw new OverdrawException("Overdraw");
				} else {
					from.withdraw(amount);
					to.deposit(amount);
				}
			}
		}

	}

	static class Account implements bank.Account {
		public String number;
		public String owner;
		private double balance;
		public boolean active = true;

		Account(String owner, int number) {
			this.owner = owner;
			String s = String.format("%09d", number);
			this.number = "1-" + s + "-" + number;

		}

		@Override
		public double getBalance() {
			return balance;
		}

		@Override
		public String getOwner() {
			return owner;
		}

		@Override
		public String getNumber() {
			return number;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public void deposit(double amount) throws InactiveException {
			if (!isActive()) {
				throw new InactiveException("Inactive");
			}
			if (amount < 0) {
				throw new IllegalArgumentException("IllegalArgument");
			}
			balance += amount;
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException {
			if (!isActive())
				throw new InactiveException("Inactive");
			if (amount < 0)
				throw new IllegalArgumentException("IllegalArgument");
			if (amount > balance) {
				throw new OverdrawException("Overdraw");
			}
			balance -= amount;
		}

	}

}