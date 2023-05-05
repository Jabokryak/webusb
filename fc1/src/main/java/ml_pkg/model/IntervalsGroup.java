package ml_pkg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
//@Scope(proxyMode= ScopedProxyMode.TARGET_CLASS, value= WebApplicationContext.SCOPE_SESSION)
public class IntervalsGroup{
	private short id;
	private int one_tick_length;
	private short pixels_per_tick;
	private short number_of_ticks;
	private ArrayList<LedIntervals> led_intervals;
	private SensorIntervals sensor_intervals;

	public IntervalsGroup() {}

	public IntervalsGroup(short id, int one_tick_length, short pixels_per_tick, short number_of_ticks, ArrayList<LedIntervals> led_intervals, SensorIntervals sensor_intervals) {
		this.id					= id;
		this.one_tick_length	= one_tick_length;
		this.pixels_per_tick	= pixels_per_tick;
		this.number_of_ticks	= number_of_ticks;
		this.led_intervals		= led_intervals;
		this.sensor_intervals	= sensor_intervals;
	}

	public void setIntervalsGroup(short id, int one_tick_length, short pixels_per_tick, short number_of_ticks, ArrayList<LedIntervals> led_intervals, SensorIntervals sensor_intervals) {
		this.id					= id;
		this.one_tick_length	= one_tick_length;
		this.pixels_per_tick	= pixels_per_tick;
		this.number_of_ticks	= number_of_ticks;
		this.led_intervals		= led_intervals;
		this.sensor_intervals	= sensor_intervals;
	}

	public JsonNode getJsonNode() {
		ObjectNode ret = JsonNodeFactory.instance.objectNode()
			.put("id",				this.id)
			.put("one_tick_length",	this.one_tick_length)
			.put("pixels_per_tick",	this.pixels_per_tick)
			.put("whole_length",	this.number_of_ticks);

		if (this.led_intervals != null && this.led_intervals.size() > 0) {
			System.out.println("IntervalsGroup.getJsonNode led_intervals.size() = " + this.led_intervals.size());

			ArrayNode arrNode = JsonNodeFactory.instance.arrayNode(this.led_intervals.size());

			this.led_intervals.forEach(r -> arrNode.add(r.getJsonNode()));

			ret.putArray("led_intervals").addAll(arrNode);
		}

		if (this.sensor_intervals != null)
			this.sensor_intervals.addTimePointsToJson(ret);

		return ret;
	}

	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}

	public int getOne_tick_length() {
		return one_tick_length;
	}

	public void setOne_tick_length(int one_tick_length) {
		this.one_tick_length = one_tick_length;
	}

	public short getPixels_per_tick() {	return pixels_per_tick;	}

	public void setPixels_per_tick(short pixels_per_tick) {	this.pixels_per_tick = pixels_per_tick;	}

	public int getWhole_length() {
		return number_of_ticks;
	}

	public void setWhole_length(short number_of_ticks) {
		this.number_of_ticks = number_of_ticks;
	}

	public ArrayList<LedIntervals> getLed_intervals() {
		return led_intervals;
	}

	public void setLed_intervals(ArrayList<LedIntervals> led_intervals) {
		this.led_intervals = led_intervals;
	}

	public SensorIntervals getSensor_intervals() {
		return sensor_intervals;
	}

	public void setSensor_intervals(SensorIntervals sensor_intervals) {	this.sensor_intervals = sensor_intervals; }

	public byte getSensor_intervals_quantity() { return this.sensor_intervals == null ? 0 : this.sensor_intervals.get_time_points_quantity(); }
}
