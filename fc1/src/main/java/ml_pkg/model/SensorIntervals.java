package ml_pkg.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

public class SensorIntervals {
	private short id;
	private ArrayList<Integer> time_points;

	public SensorIntervals(short id, ArrayList<Integer> time_points) {
		this.id = id;
		this.time_points = time_points;
	}

	public SensorIntervals() {
	}

	public short getId() {
		return id;
	}

	public void addTimePointsToJson(ObjectNode parentNode) {
		if (this.time_points == null || this.time_points.size() == 0)
			return;

		ArrayNode arrNode = JsonNodeFactory.instance.arrayNode(this.time_points.size());

		this.time_points.forEach(arrNode::add);

		parentNode.putArray("sensor_intervals").addAll(arrNode);
	}

	public void setId(short id) {
		this.id = id;
	}

	public ArrayList<Integer> getTime_points() {
		return time_points;
	}

	public void setTime_points(ArrayList<Integer> time_points) {
		this.time_points = time_points;
	}

	public byte get_time_points_quantity() { return (byte) (time_points == null ? 0 : time_points.size());}
}
