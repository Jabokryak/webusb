package ml_pkg.configuration;

import ml_pkg.model.ProgramState;
import org.springframework.stereotype.Component;

@Component
public class ProgramStateBean {
	private ProgramState state;

	public void setState(ProgramState state) {this.state = state;}

	public ProgramState state() {return this.state;}

}

