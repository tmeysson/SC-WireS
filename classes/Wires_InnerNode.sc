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
