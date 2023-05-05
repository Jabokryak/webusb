package ml_pkg.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import ml_pkg.configuration.MessageFluxBean;
import ml_pkg.configuration.ProgramStateBean;
import ml_pkg.configuration.SensorsDataFluxBean;
import ml_pkg.model.*;
import ml_pkg.repository.USBDeviceRepository;
import ml_pkg.repository.WebPageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static ml_pkg.model.McCommand.*;

@Controller
public class WebPageController {
    @Autowired
    private WebPageRepository fc1Repository;

	@Autowired
	private MessageFluxBean messageFluxSink;

	@Autowired
	private SensorsDataFluxBean sensorsDataFluxBean;

    public static JsonNode formResponse(JsonNode data, JsonNode mess) {
		Map<String, JsonNode> map = new HashMap<>(2);

		if (data != null)
			map.put("data", data);

		if (mess != null)
			map.put("messages", mess);

		return JsonNodeFactory.instance.objectNode().setAll(map);
	}

    @RequestMapping("/")
    public String f() {
		//model.addAttribute("activeGroup", activeGroup);

		//Mono<Tuple2<IntervalsGroup, Answer>> activeGroupAndAnswer =
		//.zipWith(Mono.deferContextual(ctx -> Mono.just((Answer) ctx.get(Answer.key))))
		//.contextWrite(Context.of(Answer.key, new Answer(3)));;

		/*try {
			activeGroupAndAnswer.handle((tuple, sink) -> {
				model.addAttribute("activeGroup", tuple.getT1());
				model.addAttribute("findActiveGroupMessages", tuple.getT2());

				sink.complete();
			})
			.toFuture()
			.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}*/

		/*model.addAttribute(
			"findActiveGroupMessages"
			,activeGroupMono
				.then(
					Mono.deferContextual(
						ctx -> Mono.just(ctx.get(Answer.key))
					)
				)
				//.contextWrite(ctx -> ctx.put(Answer.key, "World"))
		);*/

        return "f";
    }

	@RequestMapping("/plot.html")
	public String index() {
		return "p";
	}

	@RequestMapping(value = "/getLeds", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public Flux<ServerSentEvent<JsonNode>> getLeds() {
		System.out.println("getLeds");

		return fc1Repository.findAllLeds()
			.map(r -> ServerSentEvent.<JsonNode> builder()
				.event("led")
				.data(formResponse(
					r.getJsonNode()
					,new Answer(
						"GETLEDS_FIND_LED"
						,true
						,"Find led id = " + r.getId()
							+ ", color_name = " + r.getColor_name()
							+ ", code = " + r.getCode()
					).getJsonNode()
				))
				.build()
			)
			.switchIfEmpty(Mono.defer(() -> Mono.just(
				ServerSentEvent.<JsonNode> builder()
					.event("led")
					.data(formResponse(
						null
						,new Answer(
							"GETLEDS_LEDS_NOT_FOUND"
							,true
							,"LEDs not found"
						).getJsonNode()
					))
					.build()
			)))
			.concatWithValues(
				ServerSentEvent.<JsonNode> builder()
					.event("led_close")
					.data(JsonNodeFactory.instance.objectNode())
					.build()
			);
	}

	@RequestMapping(value = "/getSensors", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public Flux<ServerSentEvent<JsonNode>> getSensors() {
		System.out.println("getSensors");

		return fc1Repository.findActiveSensors()
			.map(r -> ServerSentEvent.<JsonNode> builder()
				.event("sensor")
				.data(formResponse(
					r.getJsonNode()
					,new Answer(
						"GETSENSORS_FIND_SENSOR"
						,true
						,"Find sensor id = " + r.getId()
						+ ", description = " + r.getDescription()
						+ ", sensor_model = " + r.getSensor_model()
					).getJsonNode()
				))
				.build()
			)
			.switchIfEmpty(Mono.defer(() -> Mono.just(
				ServerSentEvent.<JsonNode> builder()
					.event("sensor")
					.data(formResponse(
						null
						,new Answer(
							"GETSENSORS_SENSOR_NOT_FOUND"
							,true
							,"Sensors not found"
						).getJsonNode()
					))
					.build()
			)))
			.concatWithValues(
				ServerSentEvent.<JsonNode> builder()
					.event("sensor_close")
					.data(JsonNodeFactory.instance.objectNode())
					.build()
			);
	}

	@RequestMapping(value = "/getHistoricalSensorData", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public Flux<ServerSentEvent<JsonNode>> getHistoricalSensorData(
		@RequestParam("fromDate")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		LocalDateTime fromDate
	) {
		System.out.println("getHistoricalSensorData");

		record SensorDataGroup(short sensor_id, short intervals_group, byte sensor_intervals_quantity) {
			public SensorDataGroup(SensorData p_sensor_data) {
				this(p_sensor_data.sensor_id(), p_sensor_data.intervals_group(), p_sensor_data.sensor_intervals_quantity());
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				SensorDataGroup that = (SensorDataGroup) o;
				return sensor_id == that.sensor_id && intervals_group == that.intervals_group;
			}

			@Override
			public int hashCode() {
				return Objects.hash(sensor_id, intervals_group);
			}
		}

		return fc1Repository.getHistoricalSensorData(fromDate)
			.groupBy(SensorDataGroup::new)
			.flatMap(gf -> gf
				.bufferTimeout(100, Duration.ofSeconds(1))
				.map(d -> {
					ArrayList<JsonNode> dataArr = new ArrayList(d.size());
					d.forEach(e -> dataArr.add(e.getJsonNode()));

					SensorDataGroup g = gf.key();

					return ServerSentEvent.<JsonNode>builder()
						.event("d")
						.data(
							JsonNodeFactory.instance.objectNode()
								.put("sensor_id", g.sensor_id)
								.put("intervals_group", g.intervals_group)
								.put("sensor_intervals_quantity", g.sensor_intervals_quantity)
								.set("data", JsonNodeFactory.instance.arrayNode().addAll(dataArr))
						)
						.build();
				})
			)
			.concatWithValues(
				ServerSentEvent.<JsonNode> builder()
					.event("close")
					.data(JsonNodeFactory.instance.objectNode())
					.build()
			);
	}

	@RequestMapping(value = "/getActiveGroup", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Mono<JsonNode> getActiveGroup() {
		System.out.println("getActiveGroup");

		Answer answ = new Answer(3);

		return fc1Repository.findActiveGroup(answ)
			.map(g -> formResponse(g.getJsonNode(), answ.getJsonNode()))
			.switchIfEmpty(Mono.defer(() -> Mono.just(formResponse(null, answ.getJsonNode()))));
	}

    @RequestMapping(value = "/saveIntervals", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
    public Mono<JsonNode> savePoints(@RequestBody JsonNode requestBody/*, WebSession webSession*/) {
        System.out.println("savePoints: " + requestBody);

		//fc1Repository.setActiveGroup(webSession.getAttribute("activeGroup"));

		ArrayList<Led> tmpLeds = new ArrayList<>();

		ArrayList<LedIntervals> ledIntervalsArr = new ArrayList<>();
		ArrayList<Integer> sensorPoints = new ArrayList<>();
		SensorIntervals sensorIntervals = new SensorIntervals((short) 0, sensorPoints);

		IntervalsGroup tmpGroup = new IntervalsGroup(
            (short) 0
			,requestBody.get("oneTickLength").asInt()
			,requestBody.get("pixelsPerTick").shortValue()
			,requestBody.get("wholeLength").shortValue()
			,ledIntervalsArr
			,sensorIntervals
        );

        for (final JsonNode objNode : requestBody.get("lines")) {
			ArrayList<Integer> onPoints = new ArrayList<>();

			short led_id		= objNode.get("led_id").shortValue();
			short slot_number	= objNode.get("slotNumber").shortValue();

			ledIntervalsArr.add(new LedIntervals(
                (short) 0
                ,led_id
                ,slot_number
                ,onPoints
            ));

			for (final JsonNode onPointNode : objNode.get("onPoints")) {
				onPoints.add(onPointNode.asInt());
			}

			tmpLeds.add(new Led(
				led_id
				,slot_number
				,objNode.get("ledCode").asText()
			));
        }

		JsonNode m = requestBody.get("magSensorPoints");

		if (m != null)
			for (final JsonNode sensorPointsNode : m) {
				sensorPoints.add(sensorPointsNode.asInt());
			}

		//webSession.getAttributes().put("activeGroup", fc1Repository.getActiveGroup());

		System.out.println("savePoints. Leds count = " + tmpLeds.size());
		System.out.println("savePoints. Led intervals count = " + ledIntervalsArr.size());
		System.out.println("savePoints. Sensor points count = " + sensorPoints.size());

		Answer answ = new Answer(4);

		return fc1Repository.saveActiveGroup(tmpGroup, tmpLeds, answ)
			.map(r -> formResponse(null, r.getJsonNode()));
    }

	@Autowired
	@Lazy
	private USBDeviceRepository USBRep;

	@Autowired
	private ProgramStateBean programStateBean;

	@RequestMapping(value = "/commands", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Mono<JsonNode> doCommands(@RequestBody JsonNode requestBody) {
		System.out.println("doCommands: " + requestBody);

		Answer answ = new Answer(1);
		String str_command = requestBody.get("command").asText();
		McCommand command = null;
		int w = 0;

		try {
			command = McCommand.valueOf(str_command);
		} catch (IllegalArgumentException e) {
			answ.addError("USBWRITE_COMMAND_NOT_SUPPORTED", "Command " + str_command + " not supported");
		}

		if (command != null) {
			if (	command == COMMON_OFF
				||	command == OFF_SENSORS
			)
				programStateBean.setState(ProgramState.NOT_ACTIVE);

			w = USBRep.usbWrite(command, answ);
		}

		long c = answ.errCount();

		return Mono.just(formResponse(
			null
			,answ.add(
				"COMMAND_SENDED"
				,"Command " + command + " sended"
					+ (c > 0 ? " with ERROR(s) count " + c : "")
					+ ", bytes written: " + w
			).getJsonNode()
		));
	}

	@RequestMapping(value = "/getOnSensorsPointNumber", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String getOnSensorsPointNumber() {
		//System.out.println("getOnSensorsPointNumber");

		byte p = fc1Repository.getOnSensorsPointNumber();
		System.out.println("getOnSensorsPointNumber = " + p);
		return p + "";
	}

	@RequestMapping(value = "/getMessageFlux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public ParallelFlux<ServerSentEvent<JsonNode>> getMessageFlux() {
		System.out.println("getMessageFlux");

		return messageFluxSink.getMessageFlux();
	}

	@RequestMapping(value = "/getSensorsData", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public ParallelFlux<ServerSentEvent<JsonNode>> getSensorsData() {
		System.out.println("getSensorsData");

		return sensorsDataFluxBean.getFlux();
	}

    /*@ExceptionHandler
    public ResponseEntity<String> handleException(java.lang.Exception ex, Model model) {
        model.addAttribute("findAllLeds_error", ex.getMessage());

        ResponseEntity response = new ResponseEntity(
            //new String("</table></form><div>" + ex.getMessage() + "</div></body></html>")
            //,null
            HttpStatus.ACCEPTED
        );

        return response;
    }*/
}

;
