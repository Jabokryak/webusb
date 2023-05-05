package ml_pkg.repository;

import ml_pkg.configuration.MessageFluxBean;
import ml_pkg.configuration.ProgramStateBean;
import ml_pkg.configuration.UsbDeviceBean;
import ml_pkg.model.Answer;
import ml_pkg.model.McCommand;
import org.hid4java.HidDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

import static ml_pkg.model.McCommand.*;
import static ml_pkg.model.ProgramState.CYCLIC_RETREIVE_SENSORS_DATA;
import static ml_pkg.other.ColoredText.*;

@Repository
public class USBDeviceRepository {
	@Autowired
	private R2dbcEntityTemplate template;

	@Autowired
	private ProgramStateBean programStateBean;

	@Autowired
	private MessageFluxBean messageFlux;

	@Autowired
	@Lazy
	private UsbDeviceBean usbDevBean;

	public int usbWrite(McCommand command, Answer answ) {
		System.out.println("In usbWrite, command = " + command
			+ ", answ.count = " + answ.count()
		);

		HidDevice hidDevice = usbDevBean.getHidDevice(false);

		if (hidDevice == null) {
			answ.addError("USBWRITE_DEVICE_NOT_FOUND", "Device not found");

			System.out.println("usbWrite. Device not found");

			return 0;
		}

		if (hidDevice.isClosed() && !hidDevice.open()) {
			answ.addError("USBWRITE_DEVICE_OPEN_ERR", "Device not open. Error: " + hidDevice.getLastErrorMessage());

			return 0;
		}

		/*Command data format
		Common section for all commands:
		-1 - USB report ID			= 2 for out
		0 - application report ID	= command index

		Common section for ON`s commands:
		1 - LSB one tick length
		2 - MSB one tick length
		3 - LSB pixels per tick
		4 - MSB pixels per tick
		5 - number of ticks
		6 - number of LEDs

		ON_LEDS (application report ID = 2) or COMMON_ON (application report ID = 6)
		5 - 7..4 slot number, 0..3 on points array length
		6 - on points at first 8 ticks
		7 - on points at next 8 ticks if number of ticks > 8, else 0

		8 - next slot number
		etc...

		ON_SENSORS (application report ID = 4) or COMMON_ON (application report ID = 6)
		i - number of time points
		i+1 - lsb first time point
		i+2 - msb first time point

		i+3 - lsb next time point
		etc...
		*/

		final byte[] arr = new byte[63];
		arr[0] = command.index();
		final int[] i = {1};
		byte report_id = 2;

		switch (command) {
			case ON_LEDS:
			case ON_SENSORS:
			case COMMON_ON:
				//System.out.println("usbWrite. Do select active group");

				short activeGroupId = 0;

				try {
					activeGroupId = template.getDatabaseClient().sql("""
							select g.id, g.one_tick_length, g.pixels_per_tick, g.number_of_ticks
							from
								intervals_group g
							where
								g.is_active"""
						)
						.map(row -> {
							Short id = row.get("id", Short.class);
							int l_one_tick_length = row.get("one_tick_length", Integer.class);
							short pixels_per_tick = row.get("pixels_per_tick", Short.class);

							arr[i[0]] = (byte) l_one_tick_length;
							arr[i[0] + 1] = (byte) (l_one_tick_length >> 8);
							arr[i[0] + 2] = (byte) pixels_per_tick;
							arr[i[0] + 3] = (byte) (pixels_per_tick >> 8);

							arr[i[0] + 4] = row.get("number_of_ticks", Short.class).byteValue();

							/*System.out.println("id = " + id
								+ ", one_tick_length = " + Byte.toUnsignedInt(arr[i[0]]) + "_" + Byte.toUnsignedInt(arr[i[0] + 1])
								+ ", pixels_per_tick = " + Byte.toUnsignedInt(arr[i[0] + 2]) + "_" + Byte.toUnsignedInt(arr[i[0] + 3])
								+ ", number_of_ticks = " + Byte.toUnsignedInt(arr[i[0] + 4])
							);*/

							i[0] = i[0] + 5;

							return id;
						})
						.one()
						.switchIfEmpty(Mono.defer(() -> {
							//System.out.println("Active group not found");

							answ.addError(
								"USBWRITE_INTERVALSGROUP_SELECT_EMPTY"
								, "Active group not found"
							);

							return Mono.empty();
						}))
						.toFuture()
						.get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();

					answ.addError("USBWRITE_SELECT_GROUP_ERR", "Error:  " + e.getMessage());

					return 0;
				}

				if (activeGroupId == 0)
					return 0;

				int number_of_LEDs_index = i[0];        //6 - number of LEDs
				i[0]++;

				if (ON_LEDS == command
					|| COMMON_ON == command
				) {
					//System.out.println("usbWrite. Do select led intervals, group id = " + activeGroupId);

					long n = 0L;

					try {
						n = template.getDatabaseClient().sql("""
								select
									l.slot_number
									,sum(case q.elem when 0 then 0 else 1 end * POWER(2, q.rn - 1))::smallint on_points_as_2bytes
								from
									led_intervals l
									,unnest(l.on_points) WITH ORDINALITY q(elem, rn)
								where
									l.group_id = $1
								group by
									l.slot_number
								order by
									l.slot_number"""
							)
							.bind("$1", activeGroupId)
							.map(row -> {
								arr[i[0]] = row.get("slot_number", Short.class).byteValue();
								short r = row.get("on_points_as_2bytes", Short.class);
								arr[i[0] + 1] = (byte) r;
								arr[i[0] + 2] = (byte) (r >> 8);

								/*System.out.println("Led interval array index: " + i[0]
									+ ", slot_number = " + Byte.toUnsignedInt(arr[i[0]])
									+ ", on_points_as_2bytes[0] = " + String.format("%1$8s", Integer.toBinaryString(Byte.toUnsignedInt(arr[i[0] + 1]))).replace(' ', '0')
									+ ", on_points_as_2bytes[1] = " + String.format("%1$8s", Integer.toBinaryString(Byte.toUnsignedInt(arr[i[0] + 2]))).replace(' ', '0')
								);*/

								i[0] = i[0] + 3;

								return true;
							})
							.all()
							.count()
							.toFuture()
							.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();

						answ.addError("USBWRITE_SELECT_LEDINTERVALS_ERR", "Error:  " + e.getMessage());

						return 0;
					}

					arr[number_of_LEDs_index] = (byte) n;

					if (n == 0)
						answ.add("USBWRITE_SELECT_LEDINTERVALS_EMPTY", "Led intervals not found");
					else
						answ.add("USBWRITE_SELECT_LEDINTERVALS_DONE", "Found " + n + " led intervals");
				}

				if (ON_SENSORS == command
					|| COMMON_ON == command
				) {
					//System.out.println("usbWrite. Do select sensors interval, group id = " + activeGroupId);

					byte n = 0;

					try {
						n = template.getDatabaseClient().sql("""
								select
									public.sort(s.time_points) time_points
									,array_length(s.time_points, 1) array_length
								from
									sensor_intervals s
								where
									s.group_id = $1
									and s.time_points is not null
									and array_length(s.time_points, 1) > 0"""
							)
							.bind("$1", activeGroupId)
							.map(row -> {
								byte c = row.get("array_length", Short.class).byteValue();

								arr[i[0]] = c;

								/*System.out.println("Sensors interval array index: " + i[0]
									+ ", array_length = " + Byte.toUnsignedInt(arr[i[0]])
								);*/

								i[0]++;

								for (int p : row.get("time_points", Integer[].class)) {
									arr[i[0]] = (byte) p;
									arr[i[0] + 1] = (byte) (p >> 8);

									/*System.out.println("lsb time_points = " + Byte.toUnsignedInt(arr[i[0]])
										+ ", msb time_points = " + Byte.toUnsignedInt(arr[i[0] + 1])
									);*/

									i[0] = i[0] + 2;
								}

								return c;
							})
							.one()
							.toFuture()
							.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();

						answ.addError("USBWRITE_SELECT_SENSORINTERVALS_ERR", "Error:  " + e.getMessage());

						return 0;
					}

					if (n == 0) {
						answ.add("USBWRITE_SELECT_SENSORINTERVALS_EMPTY", "Sensors interval not found");
					} else {
						answ.add("USBWRITE_SELECT_SENSORINTERVALS_DONE", "Found " + n + " time points");

						programStateBean.setState(CYCLIC_RETREIVE_SENSORS_DATA);
					}
				}

				break;
			case CYCLIC_RETREIVE:
				report_id = 5;
				i[0] = 1;

				break;
		}

		//System.out.println(ANSI_GREEN + "Sending report..." + ANSI_RESET);
		//System.out.println(ANSI_GREEN + "length = " + i[0] + ", arr = " + Arrays.toString(arr) + ANSI_RESET);

		//messageFlux.emit("Request with report id = " + report_id + ": " + Arrays.toString(arr));

		int bytesWritten = 0;
		int m = 0;

		do {
			if (hidDevice == null) {
				answ.addError("USBWRITE_DEVICE_DETACHED", "Device has been detached");

				System.out.println("usbWrite. Device has been detached");

				return 0;
			}

			bytesWritten = hidDevice.write(arr, i[0], report_id, false);

			System.out.println(ANSI_GREEN + "bytesWritten = " + bytesWritten + ANSI_RESET);

			m++;

			if (bytesWritten < 0) {
				System.out.println(ANSI_RED + "Error_1: " + hidDevice.getLastErrorMessage()	+ ANSI_RESET);

				if ("Устройство не подключено.".equals(hidDevice.getLastErrorMessage()))
					hidDevice = usbDevBean.getHidDevice(true);

				if (2 == m)
					answ.addError("USBWRITE_DEVICE_WRITE_ERR", "Device write error: " + hidDevice.getLastErrorMessage());
			}
		} while (bytesWritten < 0 && m < 2);

		return bytesWritten;
	}
}
