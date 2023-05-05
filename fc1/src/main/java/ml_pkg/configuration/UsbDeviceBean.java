package ml_pkg.configuration;

import org.hid4java.HidDevice;
import org.hid4java.HidServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static ml_pkg.other.ColoredText.ANSI_RESET;
import static ml_pkg.other.ColoredText.ANSI_YELLOW;

@Component
public class UsbDeviceBean {
	private static final Integer VENDOR_ID = 1155;
	private static final Integer PRODUCT_ID = 22352;

	@Autowired
	private HidServices hidServices;

	private HidDevice hidDevice;

	public HidDevice getHidDevice(boolean do_find_device) {
		System.out.println("In createHidDevice. hidServices = " + hidServices + ", do_find_device = " + do_find_device);

		if (null == hidDevice || do_find_device) {
			synchronized (this) {
				if (null == hidDevice || do_find_device) {
					hidDevice = hidServices.getHidDevice(VENDOR_ID, PRODUCT_ID, null /*SERIAL_NUMBER*/);
				}
			}
		}

		System.out.println(ANSI_YELLOW + (hidDevice == null ? "No device attached." : "Using device: " + hidDevice.getPath()) + ANSI_RESET);

		return hidDevice;
	}
}
