package bank.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Command {

	public CommandName name;
	public String[] arguments;

	//Constructor used for JSON-generation of Object (jackson)
	public Command(){
		
	}
	public Command(CommandName n, String[] arguments) {
		this.name = n;
		this.arguments = arguments;
	}

	@JsonProperty("Name")
	public CommandName getCommandName() {
		return name;
	}
}
