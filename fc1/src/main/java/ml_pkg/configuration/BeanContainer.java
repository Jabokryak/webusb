package ml_pkg.configuration;

import ml_pkg.repository.SaveDataFromSensorsToDB;
import ml_pkg.repository.USBDeviceRepository;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;
import org.hid4java.ScanMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration
public class BeanContainer {
	@Autowired
	private ActiveGroupBean activeGroupId;

	@Autowired
	private MessageFluxBean messageFlux;

	@Autowired
	private ProgramStateBean programStateBean;

	@Autowired
	private USBDeviceRepository USBRep;

	@Autowired
	private SensorsDataFluxBean sensorsDataFluxBean;

	@Autowired
	private SensorDescriptions sensorDescriptions;

	@Autowired
	private R2dbcEntityTemplate template;

	@Bean
	public HidServices hidServices() {
		System.out.println("In hidServices bean constructor");

		HidServicesSpecification spec = new HidServicesSpecification();

		// Use manual start
		spec.setAutoStart(false);

		// Use data received events
		spec.setAutoDataRead(true);
		spec.setDataReadInterval(4);
		spec.setAutoShutdown(true);
		spec.setScanInterval(300000);
		spec.setPauseInterval(5000);
		spec.setScanMode(ScanMode.SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE);

		// Get HID services using custom specification
		HidServices hidServices = HidManager.getHidServices(spec);

		hidServices.addFluxConsumer(
			new SaveDataFromSensorsToDB(
				activeGroupId
				,messageFlux
				,template
				,programStateBean
				,USBRep
				,sensorsDataFluxBean
				,sensorDescriptions
			)
		);

		// Manually start HID services
		hidServices.start();

		System.out.println("hidServices = " + hidServices);

		return hidServices;
	}
}
