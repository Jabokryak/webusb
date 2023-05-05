package ml_pkg.repository;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ml_pkg.configuration.*;
import ml_pkg.model.Answer;
import ml_pkg.model.McCommand;
import org.hid4java.event.HidServicesEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static ml_pkg.model.McCommand.*;
import static ml_pkg.model.ProgramState.*;

@Repository
public class SaveDataFromSensorsToDB implements Consumer<HidServicesEvent> {
	private ActiveGroupBean activeGroupId;
	private MessageFluxBean messageFlux;
	private R2dbcEntityTemplate template;
	private ProgramStateBean programStateBean;
	private USBDeviceRepository USBRep;
	private SensorsDataFluxBean sensorsDataFlux;
	private SensorDescriptions sensorDescriptions;

	public SaveDataFromSensorsToDB(
		ActiveGroupBean grpBean
		,MessageFluxBean msgFlux
		,R2dbcEntityTemplate template
		,ProgramStateBean programStateBean
		,USBDeviceRepository USBRep
		,SensorsDataFluxBean sensorsDataFluxBean
		,SensorDescriptions sensorDescriptions
	) {
		this.activeGroupId		= grpBean;
		this.messageFlux		= msgFlux;
		this.template			= template;
		this.programStateBean	= programStateBean;
		this.USBRep				= USBRep;
		this.sensorsDataFlux	= sensorsDataFluxBean;
		this.sensorDescriptions	= sensorDescriptions;
	}

	/*Sensor data format
    Common section for all sensors:
    0 - USB report ID
    1 - application report ID:
        7 - data from magnet sensors
        255 - error occur
    2 - hours
    3 - minutes
    4 - seconds
    5, 6 - subseconds
    7, 8 - second fraction
    9 - day

    I2C1.
    QMC5883L sensor section.
    Magnetic field induction along the axes in units of measurement depending on the resolution of the sensor
    10 - lsb x
    11 - msb x
    12 - lsb y
    13 - msb y
    14 - lsb z
    15 - msb z
    16 - status register: Data Ready Register (DRDY) 0, Overflow flag (OVL) 1, Data Skip (DOR) 3

    First TLV493D-A1B6 sensor section.
    Magnetic field induction along the axes in units 0.098 mT/LSB (12 bit)
    17 - Bx (11..4)
    18 - By (11..4)
    19 - Bz (11..4)
    20 - Temperature (11..8) 7:4, Frame Counter 3:2, Channel 1:0
    21 - Bx (3..0), By (3..0)
    22 - Reserved 7, Testmode flag 6, Parity fuse flag 5, Power-down flag 4, Bz (3..0) 3:0
    23 - Temperature (7..0)

    24..30 - second TLV493D-A1B6 sensor section

    31 - I2C1 error code

    I2C2.
    32..38 - QMC5883L sensor section
    39..45 - First TLV493D-A1B6 sensor section
    46..52 - Second TLV493D-A1B6 sensor section
    53 - I2C2 error code

    I2C3.
    54..60 - First TLV493D-A1B6 sensor section
    61..67 - Second TLV493D-A1B6 sensor section
    68 - I2C3 error code

    69 - global error code
    70 - current sensors on point number
    */
	int positionToIndex(int pos) {
		return (byte) (
				(pos == 1) ? 10
			:	(pos == 2) ? 17
			:	(pos == 3) ? 24
			:	(pos == 4) ? 32
			:	(pos == 5) ? 39
			:	(pos == 6) ? 46
			:	(pos == 7) ? 54
			:	(pos == 8) ? 61
			: 0
		);
	}

	record VarsForInsert (short sensor_description, short x, short y, short z, short t, byte s, byte e) {
		public ArrayNode addJsonArray(ArrayNode p_parent_node, short p_sensor_id) {
			return p_parent_node
				.addArray()
				.add(p_sensor_id)
				//.add(this.sensor_description)
				.add(this.x)
				.add(this.y)
				.add(this.z)
				.add(this.s)
				.add(this.e);
		}
	}

	void cyclic_retreive() {
		if (programStateBean.state() == CYCLIC_RETREIVE_SENSORS_DATA) {
			Answer answ = new Answer(1);

			int w = USBRep.usbWrite(CYCLIC_RETREIVE, answ);

			messageFlux.emit(answ);
		}
	}

	public static String unsignedBytes(byte[] p_byte_arr) {
		String s = "[";

		for(byte e : p_byte_arr)
			s+= (((int) e) & 0xff) + ", ";

		return s+= "]";
	}

	@Override
	public void accept(@NotNull HidServicesEvent event) {
		byte[] l_report = event.getDataReceived();

		String l_thread_id = Thread.currentThread().getName() + " (id=" + Thread.currentThread().getId() + ", event_id= " + event + ")";

		System.out.println("DBConsumer.accept, thread id = " + l_thread_id
			+ ", length = " + l_report.length
			+ " :" + Arrays.toString(l_report));
		//System.out.println("activeGroupId.getId = " + activeGroupId.getId());

		byte l_report_id = l_report[0];

		//messageFlux.emit("Mc response: " + Arrays.toString(l_report));

		McCommand command;

		switch (l_report_id) {
			case 4:		//Command result: 1 - success, >1 - error code.
			case 6:		//Command result: 1 - success, >1 - error code. Plus some data.
				command = McCommand.findByIndex(l_report[1]);

				if (command == null) {
					messageFlux.emit("SAVEDATAFROMSENSORSTODB_ACCEPT_COMMAND_NOT_SUPPORTED", "Command " + l_report[1] + " not supported", false);
					messageFlux.emit("SAVEDATAFROMSENSORSTODB_ACCEPT_REPORT_CONTENT", Arrays.toString(l_report), true);

					break;
				}

				messageFlux.emit("COMMAND_MC_RESPONSE", "Command " + command + "(" + l_report[1] + ") result " + l_report[2], l_report[2] == 1);

				byte l_command_result = l_report[2];
				//System.out.println("Get answer for command " + command + ", command result: " + l_report[2]);

				switch (command) {
					case COMMON_ON, ON_SENSORS:
						if (l_command_result == 1)
							cyclic_retreive();

						break;
					case GET_SENSORS_ERRORS:
						messageFlux.emit(
							"GET_SENSORS_ERRORS_MC_RESPONSE"
							,String.format("i2c1 = %d/%d, i2c2 = %d/%d, i2c3 = %d/%d, global = %d, sensor points position = %d"
								,l_report[3], l_report[4]
								,l_report[5], l_report[6]
								,l_report[7], l_report[8]
								,l_report[9]
								,l_report[10]
							)
							,true
						);

						break;
				}

				if (command != GET_SENSORS_ERRORS
					&& command != GET_DEBUG_DATA
					&& l_report_id == 6
				) {
					messageFlux.emit(
						"ERROR_MC_RESPONSE"
						,String.format("Errors: i2c1 = %d/%d, i2c2 = %d/%d, i2c3 = %d/%d, global = %d, sensor points position = %d"
							,l_report[3], l_report[4]
							,l_report[5], l_report[6]
							,l_report[7], l_report[8]
							,l_report[9]
							,l_report[10]
						)
						,false
					);

					Answer answ = new Answer(1);

					int w = USBRep.usbWrite(GET_DEBUG_DATA, answ);

					messageFlux.emit(answ);
				}

				/*try {
					Thread.currentThread().sleep(50000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/

				break;
			case 7:		//Debug data
				messageFlux.emit("GET_DEBUG_DATA_MC_RESPONSE", unsignedBytes(l_report),true);

				break;
			case 1:		//Sensors data present
				cyclic_retreive();

				LocalDate l_current_date = LocalDate.now();

				int l_microseconds = (l_report[7] & 255) << 8 | l_report[8] & 255;
				l_microseconds = 1000000 * (l_microseconds - ((l_report[5] & 255) << 8 | l_report[6] & 255)) / (l_microseconds + 1);

				String l_timestamp = String.format("%04d-%02d-%02x %02x:%02x:%02x.%06d"
					, l_current_date.getYear()
					, l_current_date.getMonthValue()
					, l_report[9]
					, l_report[2]
					, l_report[3]
					, l_report[4]
					, l_microseconds
				);

				short l_sensor_interval_number = l_report[70];

				Consumer<VarsForInsert> insert = d -> {
					System.out.printf("%s sensor_description=%d, x=%d, y=%d, z=%d, temp=%d, status_register=%s, error=%d%n"
						, l_thread_id, d.sensor_description, d.x, d.y, d.z, d.t
						, String.format("%1$8s", Integer.toBinaryString(d.s)).replace(' ', '0')
						, d.e
					);

					try {
						template.getDatabaseClient().sql(
								"INSERT INTO sensor_data("
									+ "sensor_description, measurement_time, x, y, z, temperature, status_register, error, intervals_group, sensor_interval_number)"
									+ "	VALUES ($1, TO_TIMESTAMP($2, 'YYYY-MM-DD HH24:MI:SS.US'), $3, $4, $5, $6, $7, $8, $9, $10)"
							)
							.bind("$1", d.sensor_description)
							.bind("$2", l_timestamp)
							.bind("$3", d.x)
							.bind("$4", d.y)
							.bind("$5", d.z)
							.bind("$6", d.t)
							.bind("$7", d.s)
							.bind("$8", d.e)
							.bind("$9", activeGroupId.getId())
							.bind("$10", l_sensor_interval_number)
							.fetch()
							.one()
							.toFuture()
							.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				};

				ObjectNode sensorsDataForWeb = JsonNodeFactory.instance.objectNode()
					.put("pos", l_sensor_interval_number)
					.put("intGrp", activeGroupId.getId());

				ArrayNode sensorsDataForWebArr = sensorsDataForWeb.putArray("arr");

				IntConsumer save_QMC5883L_data = pos -> {
					int ind = positionToIndex(pos);
					SensorDescriptions.SensorDescription sensorDesc = this.sensorDescriptions.getSensorDescription(pos);

					VarsForInsert sensorData = new VarsForInsert(
						sensorDesc.id()
						, (short) ((short) l_report[ind + 1] << 8 | (short) l_report[ind] & 0xFF)
						, (short) ((short) l_report[ind + 3] << 8 | (short) l_report[ind + 2] & 0xFF)
						, (short) ((short) l_report[ind + 5] << 8 | (short) l_report[ind + 4] & 0xFF)
						, (short) 0
						, l_report[ind + 6]
						, (l_report[ind + 21] != 0) ? l_report[ind + 21] : l_report[69]
					);

					insert.accept(sensorData);

					sensorData.addJsonArray(sensorsDataForWebArr, sensorDesc.sensor_id());
				};

				IntConsumer save_TLV493D_A1B6_data = pos -> {
					int ind = positionToIndex(pos);
					SensorDescriptions.SensorDescription sensorDesc = this.sensorDescriptions.getSensorDescription(pos);

					VarsForInsert sensorData = new VarsForInsert(
						sensorDesc.id()
						, (short) (((short) (l_report[ind] << 8) | (short) (l_report[ind + 4] & 0xF0)) >> 4)
						, (short) (((short) (l_report[ind + 1] << 8) | (short) ((l_report[ind + 4] & 0x0F) << 4)) >> 4)
						, (short) (((short) (l_report[ind + 2] << 8) | (short) ((l_report[ind + 5] & 0x0F) << 4)) >> 4)
						, (short) (((short) ((l_report[ind + 3] & 0xF0) << 8) | (short) (l_report[ind + 6] << 4)) >> 4)
						, (byte) (l_report[ind + 5] & 0xF0 | l_report[ind + 3] & 0x0F)
						, ((ind == 17 || ind == 24) && l_report[31] != 0) ? l_report[31]
						: ((ind == 39 || ind == 46) && l_report[53] != 0) ? l_report[53]
						: ((ind == 54 || ind == 61) && l_report[68] != 0) ? l_report[68]
						: l_report[69]
					);

					insert.accept(sensorData);

					sensorData.addJsonArray(sensorsDataForWebArr, sensorDesc.sensor_id());
				};

				System.out.printf("%s%s, app. report: %d, global_error=%d%n", l_thread_id, l_timestamp, l_report[1], l_report[69]);

				save_QMC5883L_data.accept(1);
				save_TLV493D_A1B6_data.accept(2);
				save_TLV493D_A1B6_data.accept(3);

				save_QMC5883L_data.accept(4);
				save_TLV493D_A1B6_data.accept(5);
				save_TLV493D_A1B6_data.accept(6);

				save_TLV493D_A1B6_data.accept(7);
				save_TLV493D_A1B6_data.accept(8);

				this.sensorsDataFlux.emit(sensorsDataForWeb);

				break;
			case 3:		//No sensors data
				System.out.println("No sensors data");

				break;
			default:
				messageFlux.emit("SAVEDATAFROMSENSORSTODB_ACCEPT_REPORT_ID_NOT_SUPPORTED", "Report ID = " + l_report[0] + " not supported", false);
		}
	}
}
