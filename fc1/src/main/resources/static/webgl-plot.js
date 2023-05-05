class ColorRGBA {
  constructor(r, g, b, a = 1) {
    this.r = r;
    this.g = g;
    this.b = b;
    this.a = a;
  }
}

class ColorHSLA {
  constructor(h, s, l, a = 1) {
    this.h = h;
    this.s = s;
    this.l = l;
    this.a = a;
  }

  // input: h as an angle in [0,360] and s,l in [0,1] - output: r,g,b in [0,1]
  hsl2rgb() {
     const q = this.s * Math.min(this.l, 1 - this.l);
     const f = (n, k = (n + this.h/30)%12) => this.l - q * Math.max(Math.min(k - 3, 9 - k, 1), -1);
     return new ColorRGBA(f(0), f(8), f(4), this.a);
  }
  static hsl2rgb([h, s, l, a]) {
     const q = s * Math.min(l, 1 - l);
     const f = (n, k = (n + h/30)%12) => l - q * Math.max(Math.min(k - 3, 9 - k, 1), -1);
     return [f(0), f(8), f(4), a];
  }
}

class WebglBase {
  constructor() {
    this.scaleX = 1;
    this.scaleY = 1;
    this.offsetX = 0;
    this.offsetY = 0;
    this.loop = false;
    this._vbuffer = 0;
    this._coord = 0;
    this.visible = true;
    this.intensity = 1;
    this.xy = new Float32Array([]);
    this.numPoints = 0;
    this.color = new ColorRGBA(0, 0, 0, 1);
    this.webglNumPoints = 0;
  }
}
class WebglLine extends WebglBase {
  constructor(c, numPoints, xy) {
    super();
    this.currentIndex = 0;
    this.webglNumPoints = numPoints;
    this.numPoints = numPoints;
    this.color = c;
    this.xy = xy;  //new Float32Array(2 * this.webglNumPoints);
  }
  setX(index, x) {
    this.xy[index * 2] = x;
  }
  setY(index, y) {
    this.xy[index * 2 + 1] = y;
  }
  getX(index) {
    return this.xy[index * 2];
  }
  getY(index) {
    return this.xy[index * 2 + 1];
  }
  lineSpaceX(start, stepSize) {
    for (let i = 0; i < this.numPoints; i++) {
      this.setX(i, start + stepSize * i);
    }
  }
  arrangeX() {
    this.lineSpaceX(-1, 2 / this.numPoints);
  }
  constY(c) {
    for (let i = 0; i < this.numPoints; i++) {
      this.setY(i, c);
    }
  }
  shiftAdd(data) {
    const shiftSize = data.length;
    for (let i = 0; i < this.numPoints - shiftSize; i++) {
      this.setY(i, this.getY(i + shiftSize));
    }
    for (let i = 0; i < shiftSize; i++) {
      this.setY(i + this.numPoints - shiftSize, data[i]);
    }
  }
  addArrayY(yArray) {
    if (this.currentIndex + yArray.length <= this.numPoints) {
      for (let i = 0; i < yArray.length; i++) {
        this.setY(this.currentIndex, yArray[i]);
        this.currentIndex++;
      }
    }
  }
  replaceArrayY(yArray) {
    if (yArray.length == this.numPoints) {
      for (let i = 0; i < this.numPoints; i++) {
        this.setY(i, yArray[i]);
      }
    }
  }
}
class WebglStep extends WebglBase {
  constructor(c, num) {
    super();
    this.webglNumPoints = num * 2;
    this.numPoints = num;
    this.color = c;
    this.xy = new Float32Array(2 * this.webglNumPoints);
  }
  setY(index, y) {
    this.xy[index * 4 + 1] = y;
    this.xy[index * 4 + 3] = y;
  }
  getX(index) {
    return this.xy[index * 4];
  }
  getY(index) {
    return this.xy[index * 4 + 1];
  }
  lineSpaceX(start, stepsize) {
    for (let i = 0; i < this.numPoints; i++) {
      this.xy[i * 4] = start + i * stepsize;
      this.xy[i * 4 + 2] = start + (i * stepsize + stepsize);
    }
  }
  constY(c) {
    for (let i = 0; i < this.numPoints; i++) {
      this.setY(i, c);
    }
  }
  shiftAdd(data) {
    const shiftSize = data.length;
    for (let i = 0; i < this.numPoints - shiftSize; i++) {
      this.setY(i, this.getY(i + shiftSize));
    }
    for (let i = 0; i < shiftSize; i++) {
      this.setY(i + this.numPoints - shiftSize, data[i]);
    }
  }
}
class WebglPolar extends WebglBase {
  constructor(c, numPoints) {
    super();
    this.webglNumPoints = numPoints;
    this.numPoints = numPoints;
    this.color = c;
    this.intenisty = 1;
    this.xy = new Float32Array(2 * this.webglNumPoints);
    this._vbuffer = 0;
    this._coord = 0;
    this.visible = true;
    this.offsetTheta = 0;
  }
  setRtheta(index, theta, r) {
    const x = r * Math.cos(2 * Math.PI * (theta + this.offsetTheta) / 360);
    const y = r * Math.sin(2 * Math.PI * (theta + this.offsetTheta) / 360);
    this.setX(index, x);
    this.setY(index, y);
  }
  getTheta(index) {
    return 0;
  }
  getR(index) {
    return Math.sqrt(Math.pow(this.getX(index), 2) + Math.pow(this.getY(index), 2));
  }
  setX(index, x) {
    this.xy[index * 2] = x;
  }
  setY(index, y) {
    this.xy[index * 2 + 1] = y;
  }
  getX(index) {
    return this.xy[index * 2];
  }
  getY(index) {
    return this.xy[index * 2 + 1];
  }
}
class WebglSquare extends WebglBase {
  constructor(c, xy) {
    super();
    this.webglNumPoints = 4;
    this.numPoints = 4;
    this.color = c;
    this.xy = xy; //new Float32Array(2 * this.webglNumPoints);
  }
  setSquare(x1, y1, x2, y2) {
    this.xy = new Float32Array([x1, y1, x1, y2, x2, y1, x2, y2]);
  }
}
const scaleAndAdd = (a, b, scale) => {
  const out = {x: 0, y: 0};
  out.x = a.x + b.x * scale;
  out.y = a.y + b.y * scale;
  return out;
};
const normal = (dir) => {
  const out = set(-dir.y, dir.x);
  return out;
};
const direction = (a, b) => {
  let out = subtract(a, b);
  out = normalize(out);
  return out;
};
const add = (a, b) => {
  const out = {x: 0, y: 0};
  out.x = a.x + b.x;
  out.y = a.y + b.y;
  return out;
};
const dot = (a, b) => {
  return a.x * b.x + a.y * b.y;
};
const normalize = (a) => {
  const out = {x: 0, y: 0};
  let len = a.x * a.x + a.y * a.y;
  if (len > 0) {
    len = 1 / Math.sqrt(len);
    out.x = a.x * len;
    out.y = a.y * len;
  }
  return out;
};
const set = (x, y) => {
  const out = {x: 0, y: 0};
  out.x = x;
  out.y = y;
  return out;
};
const subtract = (a, b) => {
  const out = {x: 0, y: 0};
  out.x = a.x - b.x;
  out.y = a.y - b.y;
  return out;
};
const PolyLine = (lineXY) => {
  let curNormal;
  let lineA = {x: 0, y: 0};
  let lineB = {x: 0, y: 0};
  const out = [];
  const addNext = (normal2, length) => {
    out.push({vec2: normal2, miterLength: length});
  };
  const getXY = (index) => {
    return {x: lineXY[index * 2], y: lineXY[index * 2 + 1]};
  };
  lineA = direction(getXY(1), getXY(0));
  curNormal = normal(lineA);
  addNext(curNormal, 1);
  const numPoints = lineXY.length / 2;
  for (let i = 1; i < numPoints - 1; i++) {
    const last = getXY(i - 1);
    const cur = getXY(i);
    const next = getXY(i + 1);
    lineA = direction(cur, last);
    curNormal = normal(lineA);
    lineB = direction(next, cur);
    const miter = computeMiter(lineA, lineB);
    const miterLen = computeMiterLen(lineA, miter, 1);
    addNext(miter, miterLen);
  }
  lineA = direction(getXY(numPoints - 1), getXY(numPoints - 2));
  curNormal = normal(lineA);
  addNext(curNormal, 1);
  return out;
};
const computeMiter = (lineA, lineB) => {
  let tangent = add(lineA, lineB);
  tangent = normalize(tangent);
  const miter = set(-tangent.y, tangent.x);
  return miter;
};
const computeMiterLen = (lineA, miter, halfThick) => {
  const tmp = set(-lineA.y, lineA.x);
  return halfThick / dot(miter, tmp);
};
class WebglThickLine extends WebglBase {
  constructor(c, numPoints, thickness) {
    super();
    this.currentIndex = 0;
    this._thicknessRequested = 0;
    this._actualThickness = 0;
    this.webglNumPoints = numPoints * 2;
    this.numPoints = numPoints;
    this.color = c;
    this._thicknessRequested = thickness;
    this._linePoints = new Float32Array(numPoints * 2);
    this.xy = new Float32Array(2 * this.webglNumPoints);
  }
  convertToTriPoints() {
    const halfThick = this._actualThickness / 2;
    const normals = PolyLine(this._linePoints);
    for (let i = 0; i < this.numPoints; i++) {
      const x = this._linePoints[2 * i];
      const y = this._linePoints[2 * i + 1];
      const point = {x, y};
      const top = scaleAndAdd(point, normals[i].vec2, normals[i].miterLength * halfThick);
      const bot = scaleAndAdd(point, normals[i].vec2, -normals[i].miterLength * halfThick);
      this.xy[i * 4] = top.x;
      this.xy[i * 4 + 1] = top.y;
      this.xy[i * 4 + 2] = bot.x;
      this.xy[i * 4 + 3] = bot.y;
    }
  }
  setX(index, x) {
    this._linePoints[index * 2] = x;
  }
  setY(index, y) {
    this._linePoints[index * 2 + 1] = y;
  }
  lineSpaceX(start, stepSize) {
    for (let i = 0; i < this.numPoints; i++) {
      this.setX(i, start + stepSize * i);
    }
  }
  setThickness(thickness) {
    this._thicknessRequested = thickness;
  }
  getThickness() {
    return this._thicknessRequested;
  }
  setActualThickness(thickness) {
    this._actualThickness = thickness;
  }
}

class ColoredHead {
    constructor ({
        points_viewPlot
        ,lines_viewPlot
        ,numberOfPoints         = 10
        ,xNormalizationFactor   = 1
        ,yNormalizationFactor   = 1
        ,startColor             = new ColorHSLA(0, 1, 0.5, 1)  //new ColorRGBA(1, 0, 0, 1)
        ,endColor               = new ColorHSLA(120, 1, 0.26, 0.8)  //new ColorRGBA(0, 25/64, 0, 0.8)
        ,pointSize              = 0.005
        ,pointSizeXScale        = 2
        ,maxLineColor           = new ColorRGBA(234/256, 134/256, 79/256, 1)
        ,avgLineColor           = new ColorRGBA(234/256, 134/256, 169/256, 1)
        ,minLineColor           = new ColorRGBA(180/256, 97/256, 169/256, 1)
        ,printMessage		    = () => void 0
        ,xOffset				= 0
        ,xScale					= 1
        ,addTestPoints			= false
        ,maxTd
        ,avgTd
        ,minTd
        ,deltaTd
    }) {
        this.points_viewPlot		= points_viewPlot;
        this.lines_viewPlot			= lines_viewPlot;
        this.numberOfPoints         = numberOfPoints;
        this.xNormalizationFactor   = xNormalizationFactor;
        this.yNormalizationFactor   = yNormalizationFactor;
        this.startColor             = startColor;
        this.endColor               = endColor;
        this.pointSize              = pointSize;
        this.pointSizeXScale        = pointSizeXScale;
        this.maxLineColor           = maxLineColor;
        this.avgLineColor           = avgLineColor;
        this.minLineColor           = minLineColor;
        this.printMessage           = printMessage;
        this.xOffset				= xOffset;
        this.xScale					= xScale;
        this.maxTd					= maxTd;
        this.avgTd					= avgTd;
        this.minTd					= minTd;
        this.deltaTd				= deltaTd;

        this.defaultColorTheHead	= true;

        this._head		= new Array(this.numberOfPoints);
        this._offset	= -1;
        this._head_point_count = 0;

        this._point_count = 0;

        this.drawColorBuffer = this.getEndToStartColorArray();
        this._scRGBA = this.drawColorBuffer.slice(-4);
        this._ecRGBA = this.drawColorBuffer.slice(0, 4);

        this.lines_viewPlot.addLine(
        	new WebglLine(
        		new ColorRGBA(0.5, 0.5, 0.5, 0.8), 2, new Float32Array([0, 0, 1, 0])
			)
		);

        this.resetMaxAvgMinLines()

        if (addTestPoints) {
			let c = new ColorRGBA(1, 0, 0);

			this.lines_viewPlot.addLine(new WebglLine(c, 2, new Float32Array([0, -1, 0, 1])));
			this.lines_viewPlot.addLine(new WebglLine(c, 2, new Float32Array([0, 1, 1, 1])));
			this.lines_viewPlot.addLine(new WebglLine(c, 2, new Float32Array([1, 1, 1, -1])));
			this.lines_viewPlot.addLine(new WebglLine(c, 2, new Float32Array([1, -1, 0, -1])));

			//points_viewPlot.resizeViewPort();

			for (let c=50, i = -c; i < c; i++) {
				this.addPoint(Math.abs(i/c), i * this.yNormalizationFactor / c)
			}

			//this.points_viewPlot.update();
		}
    }

	resetMaxAvgMinLines() {
		this.maxY	= -this.yNormalizationFactor;
		this.maxYn	= -1;

        if (this.maxLineColor) {
            if (this.maxLine === undefined) {
            	this.maxLine = new WebglLine(this.maxLineColor, 2, new Float32Array([0, 0, 1, 0]));
   	            this.lines_viewPlot.addLine(this.maxLine);
            } else {
            	this.maxLine.xy[1] = this.maxLine.xy[3] = this.maxYn;
            }
        }

		if (this.maxTd)
			this.maxTd.textContent = this.maxY;

		this.avgY			= 0;
		this.avgYn			= 0;
		this._point_count	= 0;

		if (this.avgLineColor) {
            if (this.avgLine === undefined) {
				this.avgLine    = new WebglLine(this.avgLineColor, 2, new Float32Array([0, 0, 1, 0]));
				this.lines_viewPlot.addLine(this.avgLine);
            } else {
            	this.avgLine.xy[1] = this.avgLine.xy[3] = this.avgYn;
            }
        }

		if (this.avgTd)
			this.avgTd.textContent = this.avgY;

		this.minY	= this.yNormalizationFactor;
		this.minYn	= 1;

        if (this.minLineColor) {
            if (this.minLine === undefined) {
				this.minLine    = new WebglLine(this.minLineColor, 2, new Float32Array([0, 0, 1, 0]));
				this.lines_viewPlot.addLine(this.minLine);
            } else {
            	this.minLine.xy[1] = this.minLine.xy[3] = this.minYn;
            }
        }

		if (this.minTd)
			this.minTd.textContent = this.minY;

		if (this.deltaTd)
			this.deltaTd.textContent = 0;
	}

    getEndToStartColorArray(endColor = this.endColor, startColor = this.startColor) {
    	const is_hsl = startColor instanceof ColorHSLA;

	    const fHSLorRGB	= ({h, s, l, r, g, b, a}) => is_hsl ? [h, s, l, a] : [r, g, b, a];
   		const fDelta	= (a, b, d) => [(a[0] - b[0])/d, (a[1] - b[1])/d, (a[2] - b[2])/d, (a[3] - b[3])/d];

    	const n = this.numberOfPoints;
    	const a = [];

		let c	= fHSLorRGB(endColor);
		const d	= fDelta(fHSLorRGB(startColor), c, n - 1);

		for (let i = 0; i < n; i++) {
    		a.push(...(is_hsl ? ColorHSLA.hsl2rgb(c) : c));

    		c[0] += d[0];
    		c[1] += d[1];
     		c[2] += d[2];
    		c[3] = (i === n - 1) ? d[3] : 1;
   		}

    	return a;
    }

    clear() {
    	this.points_viewPlot.points.length = this.points_viewPlot.colors.length = 0;
    	this.resetMaxAvgMinLines();
    }

    addPoint(x, y, colorTheHead = this.defaultColorTheHead) {
     	let xn = x * this.xScale / this.xNormalizationFactor + this.xOffset;
     	let yn = y / this.yNormalizationFactor;

     	let deltaIsChange = false;

     	//console.log("webglPlot.ColoredHead.addPoint " + x + " " + y);

       	if (this.maxY < y) {
       		this.maxY = y;

    	    if (yn > 1) {
    	        this.maxYn = 1;

    	        this.printMessage(
    	            "VALUE_OVERSIZE_FOR_PLOT"
    	            ,`The passed value of the variable: ${y} exceeds the maximum displayed value ${this.yNormalizationFactor}`
                );
    	    } else
    	        this.maxYn = yn;

    	    if (this.maxLine)
    	    	this.maxLine.xy[1] = this.maxLine.xy[3] = this.maxYn;

    	    if (this.maxTd)
    	    	this.maxTd.textContent = this.maxY;

    	    deltaIsChange = true;
    	}

    	if (this.avgY !== y) {
    		this.avgY = (this.avgY * this._point_count + y) / (this._point_count + 1);
    	    this.avgYn = (this.avgYn * this._point_count + yn) / (this._point_count + 1);

    	    this._point_count++;

    	    if (this.avgLine)
    	    	this.avgLine.xy[1] = this.avgLine.xy[3] = this.avgYn;

    	    if (this.avgTd)
    	    	this.avgTd.textContent = Math.round(this.avgY);
		}

    	if (this.minY > y) {
    	    this.minY = y;

    	    if (yn < -1) {
    	        this.minYn = -1;

    	        this.printMessage(
    	            "VALUE_OVERSIZE_MIN_FOR_PLOT"
    	            ,`The passed value of the variable: ${y} exceeds the minimum displayed value ${-this.yNormalizationFactor}`
                );
    	    } else
	    	    this.minYn = yn;

    	    if (this.minLine)
    	    	this.minLine.xy[1] = this.minLine.xy[3] = this.minYn;

     	    if (this.minTd)
     	    	this.minTd.textContent = this.minY;

    	    deltaIsChange = true;
   		}

		if (deltaIsChange && this.deltaTd)
			this.deltaTd.textContent = this.maxY - this.minY;

		this.points_viewPlot.addPoint([xn, yn], colorTheHead ? this._scRGBA : this._ecRGBA, !colorTheHead);

		if (colorTheHead) {
			let n = this.numberOfPoints;

			if (++this._offset >= n)
				this._offset = 0;

			this._head[this._offset] = this.points_viewPlot.points.length - 2;

			if (this._head_point_count < n)
				this._head_point_count++;

			let c = this._head_point_count;

			if (c === n) {
				let k = this._head[(this._offset + 1)%n];

				this.points_viewPlot.drawPointBuffer.push(
					this.points_viewPlot.points[k]
					,this.points_viewPlot.points[k + 1]
				);
				this.points_viewPlot.drawColorBuffer.push(1,1,1,1);
			}

			for(let i = 0, j = this._offset + 1; i + 1 < c; i++, j++) {
				j %= c;

				let k = this._head[j];

				this.points_viewPlot.drawPointBuffer.push(
					this.points_viewPlot.points[k]
					,this.points_viewPlot.points[k + 1]
				);
				this.points_viewPlot.colors[k * 2] = this.drawColorBuffer[i * 4];
				this.points_viewPlot.colors[k * 2 + 1] = this.drawColorBuffer[i * 4 + 1];
				this.points_viewPlot.colors[k * 2 + 2] = this.drawColorBuffer[i * 4 + 2];
				this.points_viewPlot.colors[k * 2 + 3] = this.drawColorBuffer[i * 4 + 3];
			}

			this.points_viewPlot.drawPointBuffer.push(xn, yn);
			this.points_viewPlot.drawColorBuffer.push(...(c === n ? this.drawColorBuffer : this.drawColorBuffer.slice(-c * 4)));
		}
    }
}

class ColoredHeadGroup {
	constructor ({
		numberOfColoredHeads	= 3
		,padSize				= 0.05
		,minMaxTable
		,...paramForColoredHead
	}) {
		this.lines_viewPlot			= paramForColoredHead.lines_viewPlot;
		this.points_viewPlot		= paramForColoredHead.points_viewPlot;
		this.numberOfColoredHeads	= numberOfColoredHeads;
		this.padSize				= padSize;

		this.heads = new Array(this.numberOfColoredHeads);

		let xLength = 2;
		let xLeftSide = -1;

		this.headXScale = (xLength - this.padSize * (this.numberOfColoredHeads + 1)) / this.numberOfColoredHeads;
		let c = (xLength - this.padSize) / this.numberOfColoredHeads;

		if (minMaxTable) {
			this.minMaxTable = minMaxTable;

			const addTr = name => {
				const tr = document.createElement("tr");
				const h = document.createElement("th");
				h.textContent = name;
				tr.appendChild(h);
				this.minMaxTable.appendChild(tr);

				return tr;
			}

			this.maxTr = addTr("Max");
			this.avgTr = addTr("Avg");
			this.minTr = addTr("Min");
			this.deltaTr = addTr("Δ");
		}

		const addTd = tr => {
			const td = document.createElement("td");
			tr.appendChild(td);

			return td;
		}

		for (let i = 0;	i < this.numberOfColoredHeads; i++) {
			this.heads[i] = new ColoredHead({
				xOffset		: xLeftSide + this.padSize + i * c
				,xScale		: this.headXScale
				,...(minMaxTable ?
					{
						maxTd		: addTd(this.maxTr)
						,avgTd		: addTd(this.avgTr)
						,minTd		: addTd(this.minTr)
						,deltaTd	: addTd(this.deltaTr)
					} : null
				)
				,...paramForColoredHead
			});
		}
	}

	addPoints(x, y, colorTheHead) {
		for (let i = 0; i < y.length; i++) {
			this.heads[i].addPoint(x, y[i], colorTheHead)
		}

		requestAnimationFrame(() => {
			this.points_viewPlot.drawPointAndColorBuffer();
			this.lines_viewPlot.update();
		});
	}

	update() {
		this.lines_viewPlot.update();
		this.points_viewPlot.update();
	}

	clear() {
		this.heads.forEach(h => h.clear());
	}
}

class ViewPlot {
  constructor(webglPlot, view) {
      this.addLine = this.addDataLine;
	  this._linesData = [];
	  this._linesAux = [];
	  this._thickLines = [];
	  this._surfaces = [];
	  this.gScaleX = 2;
	  this.gScaleY = 1;
	  this.gXYratio = 1;
	  this.gOffsetX = -1;
	  this.gOffsetY = 0;
	  this.gLog10X = false;
	  this.gLog10Y = false;
	  this.webglPlot = webglPlot;
	  this.webgl = webglPlot.webgl;
	  this.view = view;
	  this.canvas = webglPlot.canvas;
	  this.program = webglPlot.program;

	  this.drawPointBuffer = [];
	  this.drawColorBuffer = [];
	  this.points = [];
	  this.colors = [];

	  this.calcBorderSize();
	  this.calcSize();
  }
  calcBorderSize() {
	  let s = window.getComputedStyle(this.view, null);

	  this._view_border_left_width = parseFloat(s.getPropertyValue("border-left-width"));
	  this._view_border_top_width = parseFloat(s.getPropertyValue("border-top-width"));
	  this._view_border_bottom_width = parseFloat(s.getPropertyValue("border-bottom-width"));
  }

  get linesData() {
    return this._linesData;
  }
  get linesAux() {
    return this._linesAux;
  }
  get thickLines() {
    return this._thickLines;
  }
  get surfaces() {
    return this._surfaces;
  }
  _drawLines(lines) {
    const webgl = this.webgl;
    lines.forEach((line) => {
      if (line.visible) {
        webgl.useProgram(this.program);
        const uscale = webgl.getUniformLocation(this.program, "uscale");
        webgl.uniformMatrix2fv(uscale, false, new Float32Array([
          line.scaleX * this.gScaleX * (this.gLog10X ? 1 / Math.log(10) : 1),
          0,
          0,
          line.scaleY * this.gScaleY * this.gXYratio * (this.gLog10Y ? 1 / Math.log(10) : 1)
        ]));
        const uoffset = webgl.getUniformLocation(this.program, "uoffset");
        webgl.uniform2fv(uoffset, new Float32Array([line.offsetX + this.gOffsetX, line.offsetY + this.gOffsetY]));
        const isLog = webgl.getUniformLocation(this.program, "is_log");
        webgl.uniform2iv(isLog, new Int32Array([this.gLog10X ? 1 : 0, this.gLog10Y ? 1 : 0]));
        const uColor = webgl.getUniformLocation(this.program, "uColor");
        webgl.uniform4fv(uColor, [line.color.r, line.color.g, line.color.b, line.color.a]);
        webgl.bufferData(webgl.ARRAY_BUFFER, line.xy, webgl.STREAM_DRAW);
        webgl.drawArrays(line.loop ? webgl.LINE_LOOP : webgl.LINES/*LINE_STRIP*/, 0, line.webglNumPoints);
      }
    });
  }
  _drawSurfaces(squares) {
    const webgl = this.webgl;
    squares.forEach((square) => {
      if (square.visible) {
        webgl.useProgram(this.program);
        const uscale = webgl.getUniformLocation(this.program, "uscale");
        webgl.uniformMatrix2fv(uscale, false, new Float32Array([
          square.scaleX * this.gScaleX * (this.gLog10X ? 1 / Math.log(10) : 1),
          0,
          0,
          square.scaleY * this.gScaleY * this.gXYratio * (this.gLog10Y ? 1 / Math.log(10) : 1)
        ]));
        const uoffset = webgl.getUniformLocation(this.program, "uoffset");
        webgl.uniform2fv(uoffset, new Float32Array([square.offsetX + this.gOffsetX, square.offsetY + this.gOffsetY]));
        const isLog = webgl.getUniformLocation(this.program, "is_log");
        webgl.uniform2iv(isLog, new Int32Array([this.gLog10X ? 1 : 0, this.gLog10Y ? 1 : 0]));
        const uColor = webgl.getUniformLocation(this.program, "uColor");
        webgl.uniform4fv(uColor, [square.color.r, square.color.g, square.color.b, square.color.a]);
        webgl.bufferData(webgl.ARRAY_BUFFER, square.xy, webgl.STREAM_DRAW);
        webgl.drawArrays(webgl.TRIANGLE_STRIP, 0, square.webglNumPoints);
      }
    });
  }
  _drawTriangles(thickLine) {
    const webgl = this.webgl;
    webgl.bufferData(webgl.ARRAY_BUFFER, thickLine.xy, webgl.STREAM_DRAW);
    webgl.useProgram(this.program);
    const uscale = webgl.getUniformLocation(this.program, "uscale");
    webgl.uniformMatrix2fv(uscale, false, new Float32Array([
      thickLine.scaleX * this.gScaleX * (this.gLog10X ? 1 / Math.log(10) : 1),
      0,
      0,
      thickLine.scaleY * this.gScaleY * this.gXYratio * (this.gLog10Y ? 1 / Math.log(10) : 1)
    ]));
    const uoffset = webgl.getUniformLocation(this.program, "uoffset");
    webgl.uniform2fv(uoffset, new Float32Array([thickLine.offsetX + this.gOffsetX, thickLine.offsetY + this.gOffsetY]));
    const isLog = webgl.getUniformLocation(this.program, "is_log");
    webgl.uniform2iv(isLog, new Int32Array([0, 0]));
    const uColor = webgl.getUniformLocation(this.program, "uColor");
    webgl.uniform4fv(uColor, [
      thickLine.color.r,
      thickLine.color.g,
      thickLine.color.b,
      thickLine.color.a
    ]);
    webgl.drawArrays(webgl.TRIANGLE_STRIP, 0, thickLine.xy.length / 2);
  }
  _drawThickLines() {
    this._thickLines.forEach((thickLine) => {
      if (thickLine.visible) {
        const calibFactor = Math.min(this.gScaleX, this.gScaleY);
        thickLine.setActualThickness(thickLine.getThickness() / calibFactor);
        thickLine.convertToTriPoints();
        this._drawTriangles(thickLine);
      }
    });
  }
  _drawPoints() {
    if (this.webglPlot instanceof WebglPoints) {
    	this.drawPointAndColorBuffer(this.points, this.colors);
	}
  }
  update() {
	this.resizeViewPort();
    this.clear();
    this.draw();
  }
  draw() {
    this._drawLines(this.linesData);
    this._drawLines(this.linesAux);
    this._drawThickLines();
    this._drawSurfaces(this.surfaces);
    this._drawPoints();
  }
  clear() {
    this.webgl.clear(this.webgl.COLOR_BUFFER_BIT);
  }
  _addLine(line) {
    line._vbuffer = this.webgl.createBuffer();
    this.webgl.bindBuffer(this.webgl.ARRAY_BUFFER, line._vbuffer);
    this.webgl.bufferData(this.webgl.ARRAY_BUFFER, line.xy, this.webgl.STREAM_DRAW);
    line._coord = this.webgl.getAttribLocation(this.program, "coordinates");
    this.webgl.vertexAttribPointer(line._coord, 2, this.webgl.FLOAT, false, 0, 0);
    this.webgl.enableVertexAttribArray(line._coord);
  }
  addDataLine(line) {
    this._addLine(line);
    this.linesData.push(line);
  }
  addAuxLine(line) {
    this._addLine(line);
    this.linesAux.push(line);
  }
  addThickLine(thickLine) {
    this._addLine(thickLine);
    this._thickLines.push(thickLine);
  }
  addSurface(surface) {
    this._addLine(surface);
    this.surfaces.push(surface);
  }
  popDataLine() {
    this.linesData.pop();
  }
  removeAllLines() {
    this._linesData = [];
    this._linesAux = [];
    this._thickLines = [];
    this._surfaces = [];
  }
  removeDataLines() {
    this._linesData = [];
  }
  removeAuxLines() {
    this._linesAux = [];
  }
  viewport(a, b, c, d) {
    this.webgl.viewport(a, b, c, d);
  }
  addPoint(coordinate_arr, color_arr, addInBuffer) {
    this.points.push(...coordinate_arr);
    this.colors.push(...color_arr);

    if (addInBuffer) {
      this.drawPointBuffer.push(...coordinate_arr);
      this.drawColorBuffer.push(...color_arr);
    }
  }
  drawPointAndColorBuffer(coordinate_arr = this.drawPointBuffer, color_arr = this.drawColorBuffer) {
  	const g = this.webgl;

  	if (g.currentViewPort !== this) {
  	 this.resizeViewPort();
  	 g.currentViewPort = this;
    }

	g.bindBuffer(g.ARRAY_BUFFER, g.a_PositionBuffer);
	g.bufferData(g.ARRAY_BUFFER, new Float32Array(coordinate_arr), g.STATIC_DRAW);

	g.bindBuffer(g.ARRAY_BUFFER, g.a_ColorBuffer);
	g.bufferData(g.ARRAY_BUFFER, new Float32Array(color_arr), g.STATIC_DRAW);

	// Draw the point
	//g.clearColor(0, 0, 0, 1);
	//g.clear(g.COLOR_BUFFER_BIT);
	g.drawArrays(g.POINTS, 0, coordinate_arr.length / 2);

	this.drawPointBuffer = [];
	this.drawColorBuffer = [];
  }
  calcSize(p_view = this.view, assign_to_this = true) {
	let s = {
	  width		: p_view.scrollWidth
	  ,height	: p_view.scrollHeight
	  ,left		: p_view.offsetLeft - this.canvas.offsetLeft + this._view_border_left_width
	  ,bottom	: this.canvas.offsetTop + this.canvas.scrollHeight - p_view.offsetTop - p_view.scrollHeight
						- this._view_border_bottom_width
	};

	if (assign_to_this) {
	  this.left		= s.left;
	  this.bottom	= s.bottom;
	  this.width	= s.width;
	  this.height	= s.height;
	}

	return s;
  }
  resizeViewPort(p_view = this.view) {
  		let s = this.calcSize(p_view);

		//p_view.textContent = `left: ${s.left}\nbottom: ${s.bottom}\nwidth: ${s.width}\nheight: ${s.height}`;
		this.webgl.viewport(this.left, this.bottom, this.width, this.height);
		this.webgl.scissor(this.left, this.bottom, this.width, this.height);
  }
}

class WebglPoints {
	constructor(canvas, point_size) {
		this.canvas = canvas;
		this.point_size = point_size;

		const gl = canvas.getContext('webgl2', {
			antialias: false
			,preserveDrawingBuffer: true
		});

		if (!gl)
			throw Error("Sorry, WebGL2 is not supported in this browser");

		this.webgl = gl;

		// Compile the vertex shader
		const vertexShaderSource = `#version 300 es
			uniform float u_PointSize;
			in vec2 a_Position;
			in vec4 a_Color;
			out vec4 v_Color;
			void main() {
				v_Color = a_Color;
				gl_Position = vec4(a_Position, 0, 1);
				gl_PointSize = u_PointSize;
			}`;
		const vs = gl.createShader(gl.VERTEX_SHADER);
		gl.shaderSource(vs, vertexShaderSource);
		gl.compileShader(vs);

		// Compile the fragment shader
		const fragmentShaderSource = `#version 300 es
			precision highp float;
			in vec4 v_Color;
			out vec4 color;
			void main() {
				color = v_Color;
			}`;
		const fs = gl.createShader(gl.FRAGMENT_SHADER);
		gl.shaderSource(fs, fragmentShaderSource);
		gl.compileShader(fs);

		// Link the program
		const prog = gl.createProgram();
		this.program = prog;
		gl.attachShader(prog, vs);
		gl.attachShader(prog, fs);
		gl.linkProgram(prog);
		if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
			console.error('prog info-log:', gl.getProgramInfoLog(prog));
			console.error('vert info-log: ', gl.getShaderInfoLog(vs));
			console.error('frag info-log: ', gl.getShaderInfoLog(fs));
		}

		// Use the program
		gl.useProgram(prog);

		// Get uniform location
		const u_PointSize = gl.getUniformLocation(prog, 'u_PointSize');

		// Set uniform value
		gl.uniform1f(u_PointSize, point_size);

		// Get attribute locations
		this.a_PositionIndex = gl.getAttribLocation(prog, 'a_Position');
		this.a_ColorIndex = gl.getAttribLocation(prog, 'a_Color');

		this.bindBuffers();
		gl.enable(gl.BLEND);
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
		gl.enable(gl.SCISSOR_TEST);
	}

	bindBuffers() {
		const gl = this.webgl;

		// Set up attribute buffers
		gl.a_PositionBuffer = gl.createBuffer();
		gl.a_ColorBuffer = gl.createBuffer();

		// Set up a vertex array object
		// This tells WebGL how to iterate your attribute buffers
		const vao = gl.createVertexArray();
		gl.bindVertexArray(vao);

		// Pull 2 floats at a time out of the position buffer
		gl.bindBuffer(gl.ARRAY_BUFFER, gl.a_PositionBuffer);
		gl.enableVertexAttribArray(this.a_PositionIndex);
		gl.vertexAttribPointer(this.a_PositionIndex, 2, gl.FLOAT, false, 0, 0);

		// Pull 4 floats at a time out of the color buffer
		gl.bindBuffer(gl.ARRAY_BUFFER, gl.a_ColorBuffer);
		gl.enableVertexAttribArray(this.a_ColorIndex);
		gl.vertexAttribPointer(this.a_ColorIndex, 4, gl.FLOAT, false, 0, 0);
	}
}

class WebglPlot {
  constructor(canvas, options) {
    this.canvas = canvas;
    this.debug = false;
    if (options == void 0) {
      this.webgl = canvas.getContext("webgl", {
        antialias: false,
        transparent: false
      });
    } else {
      this.webgl = canvas.getContext("webgl", {
        antialias: options.antialias,
        transparent: options.transparent,
        desynchronized: options.deSync,
        powerPerformance: options.powerPerformance,
        preserveDrawing: options.preserveDrawing
      });
      this.debug = options.debug == void 0 ? false : options.debug;
    }
    this.log("canvas type is: " + canvas.constructor.name);
    this.log(`[webgl-plot]:width=${canvas.width}, height=${canvas.height}`);
    this.webgl.clear(this.webgl.COLOR_BUFFER_BIT);
    //this.webgl.viewport(0, 0, 5000, 5000);//canvas.width, canvas.height);
    //this.program = this.webgl.createProgram();
    this.initThinLineProgram();
    this.webgl.enable(this.webgl.BLEND);
    this.webgl.blendFunc(this.webgl.SRC_ALPHA, this.webgl.ONE_MINUS_SRC_ALPHA);
    this.webgl.enable(this.webgl.SCISSOR_TEST);
  }
  initThinLineProgram() {
    const vertCode = `
      attribute vec2 coordinates;
      uniform mat2 uscale;
      uniform vec2 uoffset;
      uniform ivec2 is_log;

      void main(void) {
         float x = (is_log[0]==1) ? log(coordinates.x) : coordinates.x;
         float y = (is_log[1]==1) ? log(coordinates.y) : coordinates.y;
         vec2 line = vec2(x, y);
         gl_Position = vec4(uscale*line + uoffset, 0.0, 1.0);
      }`;
    const vertShader = this.webgl.createShader(this.webgl.VERTEX_SHADER);
    this.webgl.shaderSource(vertShader, vertCode);
    this.webgl.compileShader(vertShader);
    const fragCode = `
         precision mediump float;
         uniform highp vec4 uColor;
         void main(void) {
            gl_FragColor =  uColor;
         }`;
    const fragShader = this.webgl.createShader(this.webgl.FRAGMENT_SHADER);
    this.webgl.shaderSource(fragShader, fragCode);
    this.webgl.compileShader(fragShader);
    this.program = this.webgl.createProgram();
    this.webgl.attachShader(this.program, vertShader);
    this.webgl.attachShader(this.program, fragShader);
    this.webgl.linkProgram(this.program);
  }
  log(str) {
    if (this.debug) {
      console.log("[webgl-plot]:" + str);
    }
  }
}
export {ColorRGBA, ColorHSLA, WebglLine, WebglPlot, WebglPolar, WebglSquare, WebglStep, WebglThickLine, ColoredHead, ColoredHeadGroup, ViewPlot, WebglPoints};
export default null;
