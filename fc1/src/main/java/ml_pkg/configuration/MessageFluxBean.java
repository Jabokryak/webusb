package ml_pkg.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import ml_pkg.model.Answer;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.ParallelFlux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
public class MessageFluxBean {
	private Sinks.Many<ServerSentEvent<JsonNode>> fluxSink;

	private ParallelFlux<ServerSentEvent<JsonNode>> messageFlux;

	public MessageFluxBean() {
		this.fluxSink = Sinks.many().multicast().directAllOrNothing();

		this.messageFlux = fluxSink.asFlux()
			.parallel()
			.runOn(Schedulers.boundedElastic());
	}

	public ParallelFlux<ServerSentEvent<JsonNode>> getMessageFlux() {
		return messageFlux;
	}

	public void emit(String mess) {
		emit("MESSAGE_FLUX", mess, true);
	}

	public void emit(String code, String mess, boolean status) {
		//System.out.println(code + " (" + status + "): " + mess);

		Sinks.EmitResult res = fluxSink.tryEmitNext(
			ServerSentEvent.<JsonNode> builder()
				.event("mess")
				.data(new Answer.OneMessage(code, status, mess).asJson())
				.build()
		);

		if (res.isFailure()) {
			System.out.println("MessageFluxBean.emit error: " + res);
			System.out.println("Code: " + code + ", mess: " + mess);
		}
	}

	public void emit(Answer answ) {
		answ.getArr().forEach(r ->
			emit(r.code(), r.message(), r.status())
		);
	}
}
