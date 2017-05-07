Wires_Node {
	// le Synth
	var synth;
	// le Group
	var group;
	// le Bus de sortie
	var <outBus;
	// la profondeur
	var depth;

	*new {|rate = 'audio', depth = 0, target|
		^super.new.nodeInit(rate, depth, target);
	}

	nodeInit {|rate, dpth, target|
		// profondeur
		depth = dpth;
		// créer le groupe d'accueil
		group = Group(target);
		// créer le Bus de sortie
		outBus = Bus.alloc(rate);
		// créer le Synth
		synth = Wires_Def.randInstance(rate, depth, outBus, group);
		// libérer le Bus à la fin
		synth.onFree {outBus.free};
	}

	*out {
		^super.new.outNodeInit;
	}

	outNodeInit {
		// créer le groupe d'accueil
		group = Group();
		// créer le Synth
		synth = Wires_Def.outDef.makeInstance(-1, nil, group);
		// libérer le Bus à la fin
		synth.onFree {this.free};
	}

	free {
		group.free;
	}

	release {
		synth.release;
	}
}