Wires_InnerNode : Wires_Node {
	*new {|rate = 'audio', depth = 0, target, varLevel = 0, typeWeights, parent, quota, isVar = false|
		if (isVar.not && 0.1.coin)
		{ ^Wires_Var.getVar(rate, depth, target, varLevel, typeWeights, parent, quota) }
		{ ^super.new(typeWeights, quota).innerNodeInit(rate, depth, target, varLevel) };
	}

	innerNodeInit {|rate, dpth, target, level|
		// profondeur
		depth = dpth;
		// niveau de variable
		varLevel = level;
		// obtenir une définition aléatoire
		def = Wires_Def.randDef(rate, typeWeights, *quota);
		// créer le Bus de sortie
		outBus = Bus.alloc(rate);
		// créer les arguments
		this.makeArgs(def, target);
		// ajouter le Bus de sortie
		args = [out: outBus] ++ args;
		// créer le Synth
		this.makeSynth(group ? target);
	}

	replace {|parent|
		^Wires_InnerNode(outBus.rate, depth, parent.subGroup, varLevel, typeWeights,
			parent, quota);
	}
}
