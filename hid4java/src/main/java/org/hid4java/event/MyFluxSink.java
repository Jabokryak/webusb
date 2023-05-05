package org.hid4java.event;

import java.util.function.Consumer;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Sinks;

public class MyFluxSink implements Consumer<Sinks.Many<HidServicesEvent>> {
	private Sinks.Many<HidServicesEvent> fluxSink;
	
	@Override
	public void accept(Sinks.Many<HidServicesEvent> pFluxSink) {
		this.fluxSink = pFluxSink;
	}
	
	public void publishEvent(HidServicesEvent event){
		System.out.println("MyFluxSink.publishEvent thread ID: " + Thread.currentThread().getId() + ", name: " + Thread.currentThread().getName());
		
		//this.fluxSink.next(event);
		this.fluxSink.tryEmitNext(event);
	}
}
