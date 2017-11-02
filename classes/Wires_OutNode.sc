Wires_OutNode : Wires_Node {
	*basicNew {|volume, mode = 'stereo'|
		var type, chan;
		# type, chan = if (mode.isNumber)
		{['chan', mode]}
		{[mode, nil]};
		^super.basicNew(Wires_Def.outDefs[type]).outNodeInit(volume, chan);
	}

	*new {|volume, node, mode = 'stereo'|
		^this.basicNew(volume, mode).start(node);
	}

	outNodeInit {|volume, chan|
		args = [vol: volume];
		// special dedicace à Compay Segundo ;-)
		if (chan.notNil) {args = args ++ [chan: chan]};
	}

	start {|node|
		super.start(outGroup, [node]);
		// synth.onFree {isRunning = false; this.free};
	}

	// release {
	// 	synth.release;
	// }
}
