Wires_OutNode : Wires_Node {
	*basicNew {|volume|
		^super.basicNew(Wires_Def.outDef).outNodeInit(volume);
	}

	*new {|volume, node|
		^this.basicNew(volume).start(node);
	}

	outNodeInit {|volume|
		args = [vol: volume];
	}

	start {|node|
		super.start(outGroup, [node]);
		// synth.onFree {isRunning = false; this.free};
	}

	// release {
	// 	synth.release;
	// }
}
