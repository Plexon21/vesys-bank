package bank.util;

public class Result {
	public CommandName name;
	public String exception = null;
	public Object resultValue;

	public Result(CommandName name, String ex, Object o) {
		this.name = name;
		this.exception = ex;
		this.resultValue = o;
	}
	
	//Constructor used for JSON-generation of Object (jackson)
	public Result() {
	}
}
