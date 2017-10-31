Wires_InnerNode : Wires_Node {
	*basicNew {|def|
		^super.basicNew(def).innerNodeInit;
	}

	*new {|def|
		^this.basicNew(def).start;
	}

	innerNodeInit {|rate, dpth, target, level|
		// créer un Bus de sortie
		outBus = Bus.alloc(def.rate);
		args = [out: outBus];
		// s'ajouter dans le pool
		pool[def.rate].add(this);
		// nombre de lecteurs
		readers = 0;
		// lecteurs potentiels et noeuds disponibles
		potential = 5;
		availableNodes[def.rate] = availableNodes[def.rate] + 5;
		availableNodes['control'] = availableNodes['control'] - def.nbSubs[0];
		availableNodes['audio'] = availableNodes['audio'] - def.nbSubs[1];
	}

	free {
		// supprimer dans pool et dans availableNodes
		availableNodes[def.rate] = availableNodes[def.rate] - potential;
		if (pool[def.rate].includes(this)) {pool[def.rate].remove(this)};
		^super.free;
	}

}
// Wires_InnerNode : Wires_Node {
// 	*new {|rate = 'audio', depth = 0, target, varLevel = 0, typeWeights, parent, quota, isVar = false|
// 		if (isVar.not && 0.1.coin)
// 		{ ^Wires_Var.getVar(rate, depth, target, varLevel, typeWeights, parent, quota) }
// 		{ ^super.new(typeWeights, quota).innerNodeInit(rate, depth, target, varLevel) };
// 	}
//
// 	innerNodeInit {|rate, dpth, target, level|
// 		// profondeur
// 		depth = dpth;
// 		// niveau de variable
// 		varLevel = level;
// 		// obtenir une définition aléatoire
// 		def = Wires_Def.randDef(rate, typeWeights, *quota);
// 		// créer le Bus de sortie
// 		outBus = Bus.alloc(rate);
// 		// créer les arguments
// 		this.makeArgs(def, target);
// 		// ajouter le Bus de sortie
// 		args = [out: outBus] ++ args;
// 		// créer le Synth
// 		this.makeSynth(group ? target);
// 		synth.onFree {
// 			isRunning = false;
// 		};
// 	}
//
// 	replace {|parent|
// 		^Wires_InnerNode(outBus.rate, depth, parent.subGroup, varLevel, typeWeights,
// 		parent, quota);
// 	}
// }
