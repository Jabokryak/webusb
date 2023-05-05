class PrintMessage {
	constructor(msgDiv) {
		this.msgDiv = msgDiv;

		this.shadowRoot = this.msgDiv.attachShadow({ mode: "open" });

		this.shadowRoot.innerHTML = `
			<style>
				#wraper_for_flex {
					display:		grid;
                    align-items:	flex-start;
				}

				#wraper_for_sticky {
					position:		sticky;
					top:			0;
					overflow:		visible;
					height:			0;
				}

				#clear_svg {
					stroke:			#9bd814;
					stroke-width:	5;
					width:			20px;
					height:			20px;
					stroke-linecap: round;
					display:		none;
					position:		relative;
					margin-left:	calc(100% - 20px);
				}

				#clear_svg:hover {
					stroke:			#ff7f08;
				}

				#wraper_for_flex:hover #clear_svg {
					display: block;
				}
			</style>
			<div id="wraper_for_flex">
				<div id="wraper_for_sticky">
					<svg id="clear_svg" viewBox="0 0 20 20">
						<line x1="5" y1="5" x2="15" y2="15"></line>
						<line x1="5" y1="15" x2="15" y2="5"></line>
					</svg>
				</div>
				<slot id="slot"></slot>
			</div>
		`;

		this.shadowRoot.getElementById("clear_svg").addEventListener(
			"click"
			,() => [...this.msgDiv.children].forEach(c => c.tagName === "SPAN" && this.msgDiv.removeChild(c))
		);
	}

	add = (msg) => {
		let msgSpan = document.createElement("span");

		msgSpan.textContent =
			(msg.time ?? new Date().toLocaleString())
			+ (msg.code !== undefined ? " " + msg.code : "")
			+ (msg.id !== undefined ? " " + msg.id : "")
			+ (msg.message !== undefined ? ", " + msg.message : msg);

		if (!(msg.status ?? true))
			msgSpan.classList.add("redtext");

		this.msgDiv.appendChild(msgSpan);
	}

	printMessages = (messages) =>
		messages.forEach(this.add);

	message = (code, message, status, time, id) =>
		this.add({code: code, message: message, status: status, time: time, id: id});


	static async sendAndReceiveData(page, data, method = "POST", printMessageObject) {
		try {
			const response = await fetch(
				page
				,{	method:	method
					,mode:	"same-origin"
					,cache:	"no-cache"
					,headers: {"Content-Type": "application/json"}
					,redirect: "follow"
					,...(method == "POST") && {body:	JSON.stringify(data)}
				}
			);

			if (response === undefined)
				throw Error("Response is empty");

			const text = await response.text();
			let json_data;

			try {
				json_data = JSON.parse(text)
			} catch(e) {}

			if (json_data !== undefined) {
				if (json_data.hasOwnProperty("messages"))
					if (printMessageObject !== undefined)
						printMessageObject.printMessages(json_data.messages);
					else
						console.log(json_data.messages);

				if (json_data.hasOwnProperty("data"))
					return json_data.data;
				else
					return json_data;
			} else
				return text;
		} catch (e) {
			window.alert(e);

			throw e;
		}
	}
}

export { PrintMessage }
