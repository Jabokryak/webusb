package ml_pkg.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.ParallelFlux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
public class SensorsDataFluxBean {
	private Sinks.Many<ServerSentEvent<JsonNode>> fluxSink;

	private ParallelFlux<ServerSentEvent<JsonNode>> messageFlux;

	public SensorsDataFluxBean() {
		this.fluxSink = Sinks.many().multicast().directAllOrNothing();//.onBackpressureBuffer();

		this.messageFlux = fluxSink.asFlux()
			.parallel()
			.runOn(Schedulers.boundedElastic());
	}

	public ParallelFlux<ServerSentEvent<JsonNode>> getFlux() {
		return messageFlux;
	}

	public void emit(JsonNode data) {
		System.out.println(data.toString());

		Sinks.EmitResult res = fluxSink.tryEmitNext(
			ServerSentEvent.<JsonNode> builder()
				.event("d")
				.data(data)
				.build()
		);

		if (res.isFailure()) {
			System.out.println("sensorsDataFluxBean.emit error: " + res);
			System.out.println("Data: " + data.asText());
		}
	}

	public void emitWaitEvent() {
		Sinks.EmitResult res = fluxSink.tryEmitNext(
			ServerSentEvent.<JsonNode>builder()
				.event("wait")
				.data(JsonNodeFactory.instance.objectNode())
				.build()
		);

		if (res.isFailure()) {
			System.out.println("sensorsDataFluxBean.emitWaitEvent error: " + res);
		}
	}
}
