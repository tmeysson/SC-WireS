Wires_OutNode : Wires_Node {
	*basicNew {|volume|
		^super.basicNew(Wires_Def.outDef).outNodeInit(volume);
	}

	*new {|volume|
		^this.basicNew(volume).start;
	}

	outNodeInit {|volume|
		args = [vol: volume];
	}

	start {
		super.start(outGroup);
		synth.onFree {isRunning = false; this.free};
	}

	release {
		synth.release;
	}
}
