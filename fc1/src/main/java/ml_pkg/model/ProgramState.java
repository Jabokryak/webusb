package ml_pkg.model;

public enum ProgramState {
	NOT_ACTIVE(1, "Пассивное состояние")
	,CYCLIC_RETREIVE_SENSORS_DATA(2, "Периодичесий опрос датчиков");

	private final byte index;
	private final String name;

	ProgramState(int idx, String name) {
		this.index	= (byte) idx;
		this.name	= name;
	}
}
