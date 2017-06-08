Wires_FeedBackNode : Wires_Node {
	*new {|depth = 0, target, varLevel = 0, typeWeights, parent, quota|
		^super.new(typeWeights, quota).feedbackNodeInit(depth, target, varLevel);
	}

	feedbackNodeInit {|dpth, target, level|
		var in;
		// profondeur
		depth = dpth;
		// niveau de variable
		varLevel = level;
		// nombre de noeuds
		numNodes = 0;
		// il n'y a pas de sous-noeuds
		subNodes = [];
		// définition
		def = Wires_Def.feedbackDef;
		// créer le Bus de sortie
		outBus = Bus.alloc('audio');
		// créer l'argument
		in = [in: Wires.instances.choose.root.outBus];
		// ajouter le volume
		args = [out: outBus, in: in];
		// créer le Synth
		this.makeSynth(target);
	}
}
