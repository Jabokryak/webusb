package ml_pkg.model;

import java.util.HashMap;
import java.util.Map;

public enum McCommand {
	OFF_LEDS(1, "Потушить светодиоды")
	,ON_LEDS(2, "Зажечь светодиоды")
	,OFF_SENSORS(3, "Остановить опрос датчиков")
	,ON_SENSORS(4, "Запуск опроса датчиков")
	,COMMON_OFF(5, "Стоп")
	,COMMON_ON(6, "Старт")
	,GET_SENSORS_ERRORS(7, "Получить ошибки датчиков")
	,GET_SENSORS_DATA(8, "Получить данные с датчиков")
	,GET_DEBUG_DATA(9, "Получить отладку")
	,CYCLIC_RETREIVE(10, "Периодический опрос датчиков");

	public static McCommand[][] pair = {
		{OFF_LEDS,				ON_LEDS}
		,{OFF_SENSORS,			ON_SENSORS}
		,{COMMON_OFF,			COMMON_ON}
		,{GET_SENSORS_ERRORS,	GET_SENSORS_DATA}
		,{GET_DEBUG_DATA}
	};

	private final byte index;
	private final String name;

	McCommand(int idx, String name) {
		this.index	= (byte) idx;
		this.name	= name;
	}

	public byte index() { return index;}
	public String getName() { return name;}
	public McCommand[] getPair() { return pair[0];}

	private static final Map<Byte, McCommand> map;

	static {
		map = new HashMap<Byte, McCommand>();
		for (McCommand c : McCommand.values()) {
			map.put(c.index, c);
		}
	}
	public static McCommand findByIndex(byte i) {
		return map.get(i);
	}
}
