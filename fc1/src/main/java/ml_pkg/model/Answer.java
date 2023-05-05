package ml_pkg.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Answer {
	public record OneMessage (LocalDateTime time, String code, boolean status, String message) {
		public OneMessage(String code, boolean status, String message) {
			this(LocalDateTime.now(), code, status, message);
		}

		public String timeString() {
			return dateFormatter.format(this.time);
		}

		public ObjectNode asJson() {
			return JsonNodeFactory.instance.objectNode()
				.put("time", dateFormatter.format(this.time()))
				.put("code", this.code())
				.put("status", this.status())
				.put("message", this.message());
		}
	};

	private ArrayList<OneMessage> arr;
	public static final String key = "answer";
	private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

	public Answer(int count) {
		this.arr = new ArrayList<>(count);
	}

	public Answer() {
		this(1);
	}

	public Answer(String code, boolean status, String message) {
		this(1);

		arr.add(new OneMessage(code, status, message));
	}

	public void add(String code, boolean status, String message) {
		arr.add(new OneMessage(code, status, message));
	}

	public Answer add(String code, String message) {
		this.add(code, true, message);

		return this;
	}

	public Answer addError(String code, String message) {
		this.add(code, false, message);

		return this;
	}

	public void changeLast(String code, boolean status, String message) {
		arr.set(arr.size() - 1, new OneMessage(code, status, message));
	}

	public int count() {
		return arr.size();
	}

	public long errCount() {
		return arr.stream().filter(r -> !r.status).count();
	}

	public ArrayList<OneMessage> getArr() {
		return arr;
	}

	public JsonNode getJsonNode() {
		if (arr.size() == 0)
			return null;

		ArrayList<JsonNode> nodeArr = new ArrayList(arr.size());

		arr.forEach(msg -> nodeArr.add(msg.asJson()));

		//System.out.println("getJsonNode: " + nodeArr);

		return JsonNodeFactory.instance.objectNode().putArray(null).addAll(nodeArr);
	}
}
