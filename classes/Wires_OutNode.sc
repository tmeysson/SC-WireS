Wires_OutNode : Wires_Node {
	*new {|volume = 0.25, typeWeights, quota|
		^super.new(typeWeights, quota).outNodeInit(volume);
	}

	outNodeInit {|volume|
		// profondeur
		depth = -1;
		// niveau de variable
		varLevel = 0;
		// définition
		def = Wires_Def.outDef;
		// créer les arguments
		this.makeArgs(def, Wires.baseGroup);
		// ajouter le volume
		args = [vol: volume] ++ args;
		// créer le Synth
		this.makeSynth(group);
		// libérer le sous-graphe à la fin
		synth.onFree {isRunning = false; this.free};
	}
}
